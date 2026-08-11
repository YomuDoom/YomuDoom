package eu.kanade.tachiyomi.ui.recommendations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.NsfwContentFilter
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.OnlineMangaFetcher
import eu.kanade.tachiyomi.ui.browse.source.similar.SimilarMangaScorer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.runtime.State as ComposeState

class RecommendationsScreenModel(
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val historyRepository: HistoryRepository = Injekt.get(),
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
) : StateScreenModel<RecommendationsScreenModel.State>(State()) {

    private val mangaFetcher = OnlineMangaFetcher(networkToLocalManga)
    private val nsfwContentFilter = NsfwContentFilter(preferences = sourcePreferences)

    init {
        screenModelScope.launch {
            loadRecommendations()
        }
        screenModelScope.launch {
            nsfwContentFilter.changes()
                .drop(1)
                .collectLatest {
                    mutableState.update { state -> state.copy(results = emptyList()) }
                }
        }
    }

    fun refreshRecommendations() {
        if (state.value.isLoading) return

        mutableState.update { State(isLoading = true) }
        screenModelScope.launch {
            loadRecommendations()
        }
    }

    @Composable
    fun getManga(initialManga: Manga): ComposeState<Manga> {
        return rememberUpdatedState(initialManga)
    }

    override fun onDispose() {
        mangaFetcher.close()
        super.onDispose()
    }

    private suspend fun loadRecommendations() {
        val profile = buildProfile()
        if (profile.isEmpty()) {
            mutableState.update { it.copy(isLoading = false) }
            return
        }

        val knownIds = profile.mapTo(HashSet()) { it.manga.id }
        val knownTitles = profile.mapTo(HashSet()) { SimilarMangaScorer.normalize(it.manga.title) }
        val sources = sourceManager.getOnlineSources()
            .filter { it.lang in sourcePreferences.enabledLanguages.get() }
            .filterNot { it.id.toString() in sourcePreferences.disabledSources.get() }
            .filter { nsfwContentFilter.isSourceAllowed(it.id) }
            .distinctBy { it.id }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        mutableState.update {
            it.copy(
                profileSize = profile.size,
                totalSources = sources.size,
                isLoading = sources.isNotEmpty(),
            )
        }

        if (sources.isEmpty()) return

        sources.map { source ->
            screenModelScope.async {
                try {
                    val results = findRecommendationsInSource(
                        source = source,
                        profile = profile,
                        knownIds = knownIds,
                        knownTitles = knownTitles,
                    )
                    addResults(source.id, results)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    mutableState.update { state -> state.copy(failedSources = state.failedSources + 1) }
                } finally {
                    mutableState.update { state ->
                        val completedSources = state.completedSources + 1
                        state.copy(
                            completedSources = completedSources,
                            isLoading = completedSources < state.totalSources,
                        )
                    }
                }
            }
        }.awaitAll()
    }

    private suspend fun buildProfile(): List<ProfileEntry> {
        val history = historyRepository.getHistory("").first()
        val library = mangaRepository.getLibraryManga()
        val libraryById = library.associateBy { it.manga.id }
        val historyIds = history.map { it.mangaId }.distinct()
        val historySize = historyIds.size.coerceAtLeast(1)

        val profile = historyIds.mapIndexedNotNull { index, mangaId ->
            val manga = libraryById[mangaId]?.manga ?: getManga.await(mangaId) ?: return@mapIndexedNotNull null
            if (!nsfwContentFilter.isMangaAllowed(manga)) return@mapIndexedNotNull null
            val historyItem = history.firstOrNull { it.mangaId == mangaId }
            val recencyWeight = 1.0 + (historySize - index).toDouble() / historySize
            val durationWeight = min((historyItem?.readDuration ?: 0L) / READ_DURATION_UNIT, 0.5)
            val favoriteWeight = if (manga.favorite) 0.5 else 0.0
            ProfileEntry(manga, recencyWeight + durationWeight + favoriteWeight)
        }

        val profileIds = profile.mapTo(HashSet()) { it.manga.id }
        val favoriteLibrary = library
            .asSequence()
            .filter { it.manga.favorite && it.manga.id !in profileIds }
            .filter { nsfwContentFilter.isMangaAllowed(it.manga) }
            .sortedByDescending { it.manga.favoriteModifiedAt ?: 0L }
            .take(MAX_LIBRARY_ENTRIES)
            .map { ProfileEntry(it.manga, LIBRARY_ONLY_WEIGHT) }
            .toList()

        return (profile + favoriteLibrary).take(MAX_PROFILE_ENTRIES)
    }

    private suspend fun findRecommendationsInSource(
        source: HttpSource,
        profile: List<ProfileEntry>,
        knownIds: Set<Long>,
        knownTitles: Set<String>,
    ): List<Result> {
        val candidates = mangaFetcher.fetch(source)
            .filterNot { it.id in knownIds || SimilarMangaScorer.normalize(it.title) in knownTitles }
            .filter { nsfwContentFilter.isMangaAllowed(it) }
            .map { manga ->
                Result(
                    manga = manga,
                    sourceId = source.id,
                    sourceName = source.name,
                    language = source.lang,
                    score = score(manga, profile),
                )
            }
            .sortedWith(compareByDescending<Result> { it.score }.thenBy { it.manga.title })

        return candidates
            .filter { it.score > 0 }
            .ifEmpty { candidates }
            .take(MAX_RESULTS_PER_SOURCE)
    }

    private fun score(candidate: Manga, profile: List<ProfileEntry>): Int {
        val totalWeight = profile.sumOf { it.weight }
        if (totalWeight == 0.0) return 0

        return (profile.sumOf { SimilarMangaScorer.score(it.manga, candidate) * it.weight } / totalWeight)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun addResults(sourceId: Long, results: List<Result>) {
        mutableState.update { state ->
            val merged = state.results
                .filterNot { it.sourceId == sourceId }
                .plus(results.filter { nsfwContentFilter.isMangaAllowed(it.manga) })
                .groupBy { SimilarMangaScorer.normalize(it.manga.title) }
                .values
                .mapNotNull { candidates -> candidates.maxWithOrNull(resultComparator) }
                .sortedWith(resultComparator)

            state.copy(results = merged)
        }
    }

    @Immutable
    data class Result(
        val manga: Manga,
        val sourceId: Long,
        val sourceName: String,
        val language: String,
        val score: Int,
    )

    private data class ProfileEntry(
        val manga: Manga,
        val weight: Double,
    )

    @Immutable
    data class State(
        val profileSize: Int = 0,
        val results: List<Result> = emptyList(),
        val completedSources: Int = 0,
        val totalSources: Int = 0,
        val failedSources: Int = 0,
        val isLoading: Boolean = true,
    )

    private companion object {
        const val MAX_PROFILE_ENTRIES = 30
        const val MAX_LIBRARY_ENTRIES = 10
        const val MAX_RESULTS_PER_SOURCE = 8
        const val LIBRARY_ONLY_WEIGHT = 0.35
        const val READ_DURATION_UNIT = 600_000.0
        val resultComparator = compareByDescending<Result> { it.score }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.manga.title }
    }
}
