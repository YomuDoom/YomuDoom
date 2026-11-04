package eu.kanade.tachiyomi.data.download

import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slows down the app. The cache is invalidated by the time
 * defined in [renewInterval] as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 */
class DownloadCache(
    private val context: Context,
    private val provider: DownloadProvider = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
    private val stubSourceRepository: StubSourceRepository = Injekt.get(),
    private val database: Database = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Lazily, 1)

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = 1.hours.inWholeMilliseconds

    /**
     * The last time the cache was refreshed.
     */
    private var lastRenew = 0L
    private var renewalJob: Job? = null

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing
        .debounce(1.seconds) // Don't notify if it finishes quickly enough
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private val diskCacheFile: File
        get() = File(context.cacheDir, "dl_index_cache_v5")

    private val rootDownloadsDirMutex = Mutex()
    private var rootDownloadsDir = RootDirectory(storageManager.getDownloadsDirectory())

    init {
        // Clean legacy cache files
        try {
            File(context.cacheDir, "dl_index_cache_v3").delete()
            File(context.cacheDir, "dl_index_cache_v4").delete()
        } catch (_: Throwable) {
        }

        // Attempt to read cache file
        scope.launch {
            rootDownloadsDirMutex.withLock {
                try {
                    if (diskCacheFile.exists()) {
                        val diskCache = diskCacheFile.inputStream().use {
                            ProtoBuf.decodeFromByteArray<RootDirectory>(it.readBytes())
                        }
                        rootDownloadsDir = diskCache
                        val downloadsDir = storageManager.getDownloadsDirectory()
                        val hasPhysicalDirs = downloadsDir?.listFiles().orEmpty().any {
                            it.isDirectory &&
                                !it.name.isNullOrBlank()
                        }
                        val totalChapters = diskCache.sourceDirs.values.sumOf { sourceDir ->
                            sourceDir.mangaDirs.values.sumOf { it.chapterDirs.size }
                        }
                        if (hasPhysicalDirs && (diskCache.sourceDirs.isEmpty() || totalChapters == 0)) {
                            lastRenew = 0L
                        } else {
                            lastRenew = System.currentTimeMillis()
                        }
                    }
                } catch (e: Throwable) {
                    logcat(LogPriority.ERROR, e) { "Failed to initialize from disk cache" }
                    diskCacheFile.delete()
                }
            }
            if (lastRenew == 0L) {
                renewCache()
            } else {
                scope.launch {
                    delay(3.seconds)
                    renewCache(force = true)
                }
            }
        }

        storageManager.changes
            .onEach { invalidateCache() }
            .launchIn(scope)

        extensionManager.installedExtensionsFlow
            .drop(1)
            .distinctUntilChanged()
            .onEach { invalidateCache() }
            .launchIn(scope)
    }

    /**
     * Returns the download directory for a given source ID if cached.
     */
    fun getSourceDirectory(sourceId: Long): UniFile? {
        renewCache()
        return rootDownloadsDir.sourceDirs[sourceId]?.dir
    }

    /**
     * Returns true if the chapter is downloaded.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param chapterUrl the url of the chapter to query
     * @param mangaTitle the title of the manga to query.
     * @param sourceId the id of the source of the chapter.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isChapterDownloaded(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        mangaTitle: String,
        sourceId: Long,
        skipCache: Boolean,
    ): Boolean {
        if (skipCache) {
            val source = sourceManager.getOrStub(sourceId)
            return provider.findChapterDir(chapterName, chapterScanlator, chapterUrl, mangaTitle, source) != null
        }

        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[sourceId]
        val disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get()
        val mangaDir = sourceDir?.findMangaDir(mangaTitle, provider, disallowNonAscii)
            ?: rootDownloadsDir.sourceDirs.values.firstNotNullOfOrNull {
                it.findMangaDir(mangaTitle, provider, disallowNonAscii)
            }

        if (mangaDir != null) {
            val validNames = provider.getValidChapterDirNames(
                chapterName,
                chapterScanlator,
                chapterUrl,
            )
            val validBaseNames = validNames.map { it.removeSuffix(".cbz").removeSuffix(".zip") }

            return mangaDir.chapterDirs.any { cached ->
                val cachedBase = cached.removeSuffix(".cbz").removeSuffix(".zip")
                val cachedWithoutHash = if (cachedBase.length > 7 && cachedBase[cachedBase.length - 7] == '_') {
                    cachedBase.dropLast(7)
                } else {
                    cachedBase
                }
                validNames.any { it.equals(cached, ignoreCase = true) } ||
                    validBaseNames.any { validBase ->
                        validBase.equals(cachedBase, ignoreCase = true) ||
                            validBase.equals(cachedWithoutHash, ignoreCase = true)
                    }
            }
        }
        return false
    }

    /**
     * Returns the amount of downloaded chapters.
     */
    fun getTotalDownloadCount(): Int {
        renewCache()

        return rootDownloadsDir.sourceDirs.values.sumOf { sourceDir ->
            sourceDir.mangaDirs.values.sumOf { mangaDir ->
                mangaDir.chapterDirs.size
            }
        }
    }

    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: Manga): Int {
        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[manga.source]
        val disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get()
        val mangaDir = sourceDir?.findMangaDir(manga.title, provider, disallowNonAscii)
            ?: rootDownloadsDir.sourceDirs.values.firstNotNullOfOrNull {
                it.findMangaDir(manga.title, provider, disallowNonAscii)
            }

        return mangaDir?.chapterDirs?.size ?: 0
    }

    /**
     * Adds a chapter that has just been download to this cache.
     *
     * @param chapterDirName the downloaded chapter's directory name.
     * @param mangaUniFile the directory of the manga.
     * @param manga the manga of the chapter.
     */
    suspend fun addChapter(chapterDirName: String, mangaUniFile: UniFile, manga: Manga) {
        rootDownloadsDirMutex.withLock {
            // Retrieve the cached source directory or cache a new one
            var sourceDir = rootDownloadsDir.sourceDirs[manga.source]
            if (sourceDir == null) {
                val source = sourceManager.get(manga.source) ?: return
                val sourceUniFile = provider.findSourceDir(source) ?: return
                sourceDir = SourceDirectory(sourceUniFile)
                rootDownloadsDir.sourceDirs += manga.source to sourceDir
            }

            // Retrieve the cached manga directory or cache a new one
            val mangaDirName = provider.getMangaDirName(manga.title)
            var mangaDir = sourceDir.mangaDirs[mangaDirName]
            if (mangaDir == null) {
                mangaDir = MangaDirectory(mangaUniFile)
                sourceDir.mangaDirs += mangaDirName to mangaDir
            }

            // Save the chapter directory
            mangaDir.chapterDirs += chapterDirName
        }

        notifyChanges()
    }

    /**
     * Removes a chapter that has been deleted from this cache.
     *
     * @param chapter the chapter to remove.
     * @param manga the manga of the chapter.
     */
    suspend fun removeChapter(chapter: Chapter, manga: Manga) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
            val disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get()
            val mangaDir = sourceDir.findMangaDir(manga.title, provider, disallowNonAscii) ?: return
            provider.getValidChapterDirNames(chapter.name, chapter.scanlator, chapter.url).forEach {
                if (it in mangaDir.chapterDirs) {
                    mangaDir.chapterDirs -= it
                }
            }
        }

        notifyChanges()
    }

    /**
     * Removes a list of chapters that have been deleted from this cache.
     *
     * @param chapters the list of chapter to remove.
     * @param manga the manga of the chapter.
     */
    suspend fun removeChapters(chapters: List<Chapter>, manga: Manga) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
            val disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get()
            val mangaDir = sourceDir.findMangaDir(manga.title, provider, disallowNonAscii) ?: return
            chapters.forEach { chapter ->
                provider.getValidChapterDirNames(chapter.name, chapter.scanlator, chapter.url).forEach {
                    if (it in mangaDir.chapterDirs) {
                        mangaDir.chapterDirs -= it
                    }
                }
            }
        }

        notifyChanges()
    }

    /**
     * Removes a manga that has been deleted from this cache.
     *
     * @param manga the manga to remove.
     */
    suspend fun removeManga(manga: Manga) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
            val disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get()
            val mangaDir = sourceDir.findMangaDir(manga.title, provider, disallowNonAscii)
            if (mangaDir != null) {
                sourceDir.mangaDirs = sourceDir.mangaDirs.filterValues { it != mangaDir }
            }
        }

        notifyChanges()
    }

    /**
     * Renames a manga in this cache.
     *
     * @param manga the manga being renamed.
     * @param mangaUniFile the manga's new directory.
     * @param newTitle the manga's new title.
     */
    suspend fun renameManga(manga: Manga, mangaUniFile: UniFile, newTitle: String) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
            val oldMangaDirName = provider.getMangaDirName(manga.title)
            var oldChapterDirs: MutableSet<String>? = null
            // Save the old name's cached chapter dirs
            if (sourceDir.mangaDirs.containsKey(oldMangaDirName)) {
                oldChapterDirs = sourceDir.mangaDirs[oldMangaDirName]?.chapterDirs
                sourceDir.mangaDirs -= oldMangaDirName
            }

            // Retrieve/create the cached manga directory for new name
            val newMangaDirName = provider.getMangaDirName(newTitle)
            var mangaDir = sourceDir.mangaDirs[newMangaDirName]
            if (mangaDir == null) {
                mangaDir = MangaDirectory(mangaUniFile)
                sourceDir.mangaDirs += newMangaDirName to mangaDir
            }

            // Add the old chapters to new name's cache
            if (!oldChapterDirs.isNullOrEmpty()) {
                mangaDir.chapterDirs += oldChapterDirs
            }
        }

        notifyChanges()
    }

    suspend fun removeSource(source: Source) {
        rootDownloadsDirMutex.withLock {
            rootDownloadsDir.sourceDirs -= source.id
        }

        notifyChanges()
    }

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
        diskCacheFile.delete()
        renewCache(force = true)
    }

    /**
     * Renews the downloads cache.
     */
    private fun renewCache(force: Boolean = false) {
        // Avoid renewing cache if in the process nor too often
        if ((!force && lastRenew + renewInterval >= System.currentTimeMillis()) || renewalJob?.isActive == true) {
            return
        }

        renewalJob = scope.launchIO {
            if (lastRenew == 0L) {
                _isInitializing.emit(true)
            }

            // Try to wait until extensions and sources have loaded
            var sources = emptyList<Source>()
            withTimeoutOrNull(30.seconds) {
                extensionManager.isInitialized.first { it }
                sourceManager.isInitialized.first { it }

                sources = getSources()
            }

            val sourceMap = mutableMapOf<String, Long>()
            sources.forEach { source ->
                val primaryName = provider.getSourceDirName(source).lowercase()
                sourceMap[primaryName] = source.id
                val nameOnly = DiskUtil.buildValidFilename(source.name).lowercase()
                if (nameOnly.isNotBlank()) {
                    sourceMap.putIfAbsent(nameOnly, source.id)
                }
                if (source.lang.isNotBlank()) {
                    sourceMap.putIfAbsent("$nameOnly (${source.lang.lowercase()})", source.id)
                }
            }

            rootDownloadsDirMutex.withLock {
                val updatedRootDir = RootDirectory(storageManager.getDownloadsDirectory())
                val rawSourceDirs = updatedRootDir.dir?.listFiles().orEmpty()
                    .filter { it.isDirectory && !it.name.isNullOrBlank() }

                val resolvedDirs = mutableMapOf<Long, SourceDirectory>()
                val unmatchedDirs = mutableListOf<UniFile>()

                for (dir in rawSourceDirs) {
                    val dirName = dir.name!!.lowercase()
                    val sourceId = sourceMap[dirName]
                    if (sourceId != null) {
                        resolvedDirs[sourceId] = SourceDirectory(dir)
                    } else {
                        unmatchedDirs.add(dir)
                    }
                }

                if (unmatchedDirs.isNotEmpty()) {
                    val allManga = try {
                        database.mangasQueries.getAllMangaTitlesAndSources().awaitAsList()
                    } catch (e: Throwable) {
                        logcat(LogPriority.WARN, e) { "DownloadCache: failed to query manga for directory matching" }
                        emptyList()
                    }

                    val stubSources = sourceManager.getStubSources()

                    for (dir in unmatchedDirs) {
                        val dirName = dir.name!!
                        var resolvedSourceId: Long? = null

                        // 1. Try matching by source ID directly if folder name is numeric
                        val idFromName = dirName.toLongOrNull()
                        val isKnownId = idFromName != null && (
                            allManga.any { it.source == idFromName } ||
                                stubSources.any { it.id == idFromName }
                            )
                        if (isKnownId) {
                            resolvedSourceId = idFromName
                        }

                        // 2. Try matching with known stub sources by name prefix
                        if (resolvedSourceId == null) {
                            val matchingStub = stubSources.firstOrNull { stub ->
                                dirName.startsWith("${stub.name} (", ignoreCase = true) ||
                                    dirName.equals(stub.name, ignoreCase = true) ||
                                    stub.name.startsWith(dirName, ignoreCase = true)
                            }
                            if (matchingStub != null) {
                                resolvedSourceId = matchingStub.id
                                if (matchingStub.lang.isBlank()) {
                                    val langMatch = Regex("""\(([a-zA-Z0-9\-_]+)\)$""").find(dirName)
                                    val lang = langMatch?.groupValues?.get(1)?.lowercase().orEmpty()
                                    if (lang.isNotBlank()) {
                                        stubSourceRepository.upsertStubSource(matchingStub.id, lang, matchingStub.name)
                                    }
                                }
                            }
                        }

                        // 3. Try matching by subfolders (manga titles inside dir) against database mangas
                        if (resolvedSourceId == null && allManga.isNotEmpty()) {
                            val subDirs = dir.listFiles().orEmpty()
                                .filter { it.isDirectory && !it.name.isNullOrBlank() }
                            val subDirNames = subDirs.map { it.name!!.lowercase() }.toSet()
                            val subDirNorms = subDirs
                                .map { normalizeForMatch(it.name!!) }
                                .filter { it.isNotBlank() }
                                .toSet()

                            val matchedManga = allManga.firstOrNull { manga ->
                                val mangaDirName = provider.getMangaDirName(manga.title).lowercase()
                                val normTitle = normalizeForMatch(manga.title)
                                val normDir = normalizeForMatch(mangaDirName)
                                mangaDirName in subDirNames ||
                                    manga.title.lowercase() in subDirNames ||
                                    (normTitle.isNotBlank() && normTitle in subDirNorms) ||
                                    (normDir.isNotBlank() && normDir in subDirNorms)
                            }

                            if (matchedManga != null) {
                                resolvedSourceId = matchedManga.source
                                val langMatch = Regex("""^(.+?)(?:\s*\(([a-zA-Z0-9\-_]+)\))?$""").find(dirName)
                                val sourceName = langMatch?.groupValues?.get(1)?.trim() ?: dirName
                                val lang = langMatch?.groupValues?.getOrNull(2)?.lowercase().orEmpty()
                                stubSourceRepository.upsertStubSource(matchedManga.source, lang, sourceName)
                            }
                        }

                        if (resolvedSourceId != null) {
                            resolvedDirs[resolvedSourceId] = SourceDirectory(dir)
                            sourceMap[dirName.lowercase()] = resolvedSourceId
                        }
                    }
                }

                updatedRootDir.sourceDirs = resolvedDirs

                updatedRootDir.sourceDirs.values.map { sourceDir ->
                    async {
                        sourceDir.mangaDirs = sourceDir.dir?.listFiles().orEmpty()
                            .filter { it.isDirectory && !it.name.isNullOrBlank() }
                            .associate { it.name!! to MangaDirectory(it) }

                        sourceDir.mangaDirs.values.forEach { mangaDir ->
                            val chapterDirs = mangaDir.dir?.listFiles().orEmpty()
                                .mapNotNull {
                                    when {
                                        // Ignore incomplete downloads
                                        it.name?.endsWith(Downloader.TMP_DIR_SUFFIX) == true -> null
                                        // Folder of images
                                        it.isDirectory -> it.name
                                        // CBZ / ZIP files
                                        it.isFile && (it.extension == "cbz" || it.extension == "zip") -> {
                                            it.nameWithoutExtension
                                        }
                                        // Anything else is irrelevant
                                        else -> null
                                    }
                                }
                                .toMutableSet()

                            mangaDir.chapterDirs = chapterDirs
                        }
                    }
                }
                    .awaitAll()

                rootDownloadsDir = updatedRootDir
            }

            _isInitializing.emit(false)
        }.also {
            it.invokeOnCompletion(onCancelling = true) { exception ->
                if (exception != null && exception !is CancellationException) {
                    logcat(LogPriority.ERROR, exception) { "DownloadCache: failed to create cache" }
                }
                lastRenew = System.currentTimeMillis()
                notifyChanges()
            }
        }

        // Mainly to notify the indexing notifier UI
        notifyChanges()
    }

    private fun getSources(): List<Source> {
        return sourceManager.getOnlineSources() + sourceManager.getStubSources()
    }

    private fun notifyChanges() {
        scope.launchNonCancellable {
            _changes.send(Unit)
        }
        updateDiskCache()
    }

    private var updateDiskCacheJob: Job? = null
    private fun updateDiskCache() {
        updateDiskCacheJob?.cancel()
        updateDiskCacheJob = scope.launchIO {
            delay(1.seconds)
            ensureActive()
            val bytes = ProtoBuf.encodeToByteArray(rootDownloadsDir)
            ensureActive()
            try {
                diskCacheFile.writeBytes(bytes)
            } catch (e: Throwable) {
                logcat(
                    priority = LogPriority.ERROR,
                    throwable = e,
                    message = { "Failed to write disk cache file" },
                )
            }
        }
    }
}

/**
 * Class to store the files under the root downloads directory.
 */
@Serializable
private class RootDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var sourceDirs: Map<Long, SourceDirectory> = mapOf(),
)

/**
 * Class to store the files under a source directory.
 */
@Serializable
private class SourceDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var mangaDirs: Map<String, MangaDirectory> = mapOf(),
) {
    fun findMangaDir(
        mangaTitle: String,
        provider: DownloadProvider,
        disallowNonAscii: Boolean,
    ): MangaDirectory? {
        val defaultName = provider.getMangaDirName(mangaTitle)
        mangaDirs[defaultName]?.let { return it }

        val caseMatch = mangaDirs.entries.firstOrNull { it.key.equals(defaultName, ignoreCase = true) }
        if (caseMatch != null) return caseMatch.value

        val altName = DiskUtil.buildValidFilename(mangaTitle, disallowNonAscii = !disallowNonAscii)
        if (altName != defaultName) {
            mangaDirs[altName]?.let { return it }
            val altCase = mangaDirs.entries.firstOrNull { it.key.equals(altName, ignoreCase = true) }
            if (altCase != null) return altCase.value
        }

        mangaDirs[mangaTitle]?.let { return it }
        val titleCase = mangaDirs.entries.firstOrNull { it.key.equals(mangaTitle, ignoreCase = true) }
        if (titleCase != null) return titleCase.value

        val normTitle = normalizeForMatch(mangaTitle)
        if (normTitle.isNotBlank()) {
            val normMatch = mangaDirs.entries.firstOrNull { normalizeForMatch(it.key) == normTitle }
            if (normMatch != null) return normMatch.value
        }

        return null
    }
}

private fun normalizeForMatch(text: String): String {
    return text.lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), "")
}

/**
 * Class to store the files under a manga directory.
 */
@Serializable
private class MangaDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var chapterDirs: MutableSet<String> = mutableSetOf(),
)

private object UniFileAsStringSerializer : KSerializer<UniFile?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UniFile", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UniFile?) {
        return if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value.uri.toString())
        }
    }

    override fun deserialize(decoder: Decoder): UniFile? {
        return if (decoder.decodeNotNullMark()) {
            UniFile.fromUri(Injekt.get<Application>(), decoder.decodeString().toUri())
        } else {
            decoder.decodeNull()
        }
    }
}
