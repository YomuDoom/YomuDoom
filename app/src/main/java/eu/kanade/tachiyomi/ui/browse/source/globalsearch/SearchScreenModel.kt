package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.aallam.similarity.NormalizedLevenshtein
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.domain.source.service.GlobalSearchMode
import eu.kanade.domain.source.service.NsfwContentFilter
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.preference.toggle
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale
import java.util.concurrent.Executors

abstract class SearchScreenModel(
    initialState: State = State(),
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val preferences: SourcePreferences = Injekt.get(),
) : StateScreenModel<SearchScreenModel.State>(initialState) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private val normalizedLevenshtein = NormalizedLevenshtein()
    protected val nsfwContentFilter = NsfwContentFilter(preferences = sourcePreferences)
    private var searchJob: Job? = null

    private val enabledLanguages = sourcePreferences.enabledLanguages.get()
    private val disabledSources = sourcePreferences.disabledSources.get()
    protected val pinnedSources = sourcePreferences.pinnedSources.get()

    private var lastQuery: String? = null
    private var lastSourceFilter: SourceFilter? = null

    protected var extensionFilter: String? = null

    open val sortComparator = { map: Map<Source, SearchItemResult> ->
        if (state.value.searchMode == GlobalSearchMode.LEGACY) {
            compareByDescending<Source> { (map[it] as? SearchItemResult.Success)?.chapterCount ?: -1 }
                .thenBy { (map[it] as? SearchItemResult.Success)?.isEmpty ?: true }
                .thenBy { "${it.id}" !in pinnedSources }
                .thenBy { "${it.name.lowercase()} (${it.lang})" }
        } else {
            compareByDescending<Source> { (map[it] as? SearchItemResult.Success)?.chapterCount ?: -1 }
                .thenBy { "${it.id}" !in pinnedSources }
                .thenBy { "${it.name.lowercase()} (${it.lang})" }
        }
    }

    init {
        mutableState.update { it.copy(searchMode = preferences.globalSearchMode.get()) }
        screenModelScope.launch {
            preferences.globalSearchFilterState.changes().collectLatest { state ->
                mutableState.update { it.copy(onlyShowHasResults = state) }
            }
        }
        screenModelScope.launch {
            nsfwContentFilter.changes()
                .drop(1)
                .collectLatest {
                    searchJob?.cancel()
                    mutableState.update { it.copy(items = emptyMap(), consolidatedItems = emptyList()) }
                    if (!state.value.searchQuery.isNullOrBlank()) search()
                }
        }
    }

    @Composable
    fun getManga(initialManga: Manga): androidx.compose.runtime.State<Manga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .filterNotNull()
                .collectLatest { manga ->
                    value = manga
                }
        }
    }

    open fun getEnabledSources(): List<Source> {
        return sourceManager.getAll()
            .filter {
                it.lang in enabledLanguages &&
                    "${it.id}" !in disabledSources &&
                    nsfwContentFilter.isSourceAllowed(it.id)
            }
            .sortedWith(
                compareBy(
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    private fun getSelectedSources(): List<Source> {
        val enabledSources = getEnabledSources()

        val filter = extensionFilter
        if (filter.isNullOrEmpty()) {
            return enabledSources
        }

        return extensionManager.installedExtensionsFlow.value
            .filter { it.pkgName == filter }
            .flatMap { it.sources }
            .filter { it in enabledSources }
    }

    fun updateSearchQuery(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun setSourceFilter(filter: SourceFilter) {
        mutableState.update { it.copy(sourceFilter = filter) }
        search()
    }

    fun toggleFilterResults() {
        preferences.globalSearchFilterState.toggle()
    }

    fun setSearchMode(mode: GlobalSearchMode) {
        if (mode == state.value.searchMode) return

        preferences.globalSearchMode.set(mode)
        lastQuery = null
        mutableState.update {
            it.copy(
                searchMode = mode,
                items = emptyMap(),
                consolidatedItems = emptyList(),
            )
        }
        if (!state.value.searchQuery.isNullOrBlank()) {
            search()
        }
    }

    fun search() {
        val query = state.value.searchQuery
        val sourceFilter = state.value.sourceFilter

        if (query.isNullOrBlank()) return

        val sameQuery = this.lastQuery == query
        if (sameQuery && this.lastSourceFilter == sourceFilter) return

        this.lastQuery = query
        this.lastSourceFilter = sourceFilter

        searchJob?.cancel()

        val sources = getSelectedSources()

        // Reuse previous results if possible
        if (sameQuery) {
            val existingResults = state.value.items
            updateItems(
                sources
                    .associateWith { existingResults[it] ?: SearchItemResult.Loading },
            )
        } else {
            updateItems(
                sources
                    .associateWith { SearchItemResult.Loading },
            )
        }

        searchJob = ioCoroutineScope.launch {
            sources.map { source ->
                async {
                    if (state.value.items[source] !is SearchItemResult.Loading) {
                        return@async
                    }

                    try {
                        val page = withContext(coroutineDispatcher) {
                            source.getSearchManga(1, query, source.getFilterList())
                        }

                        val titles = page.mangas
                            .map { it.toDomainManga(source.id) }
                            .distinctBy { it.url }
                            .let { networkToLocalManga(it) }

                        val searchMatch = titles.bestSearchMatch(query)
                        val chapterCount = searchMatch?.let { getChapterCount(source, it.manga) } ?: 0

                        if (isActive) {
                            updateItem(source, SearchItemResult.Success(titles, chapterCount))
                            if (state.value.searchMode == GlobalSearchMode.CONSOLIDATED && searchMatch != null) {
                                updateConsolidatedItem(
                                    source = source,
                                    manga = searchMatch.manga,
                                    chapterCount = chapterCount,
                                    matchScore = searchMatch.score,
                                )
                            }
                        }
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException) throw throwable
                        if (isActive) {
                            updateItem(source, SearchItemResult.Error(throwable))
                        }
                    }
                }
            }
                .awaitAll()
        }
    }

    private fun updateItems(items: Map<Source, SearchItemResult>) {
        mutableState.update {
            it.copy(
                items = items
                    .toSortedMap(sortComparator(items)),
                consolidatedItems = emptyList(),
            )
        }
    }

    private fun updateItem(source: Source, result: SearchItemResult) {
        mutableState.update { current ->
            val items = current.items + (source to result)
            current.copy(items = items.toSortedMap(sortComparator(items)))
        }
    }

    private fun updateConsolidatedItem(
        source: Source,
        manga: Manga,
        chapterCount: Int,
        matchScore: Double,
    ) {
        val candidate = ConsolidatedSearchCandidate(
            manga = manga,
            source = source,
            chapterCount = chapterCount,
            matchScore = matchScore,
        )

        mutableState.update { current ->
            val matchingIndex = current.consolidatedItems.indexOfFirst {
                sameWork(it.manga.title, candidate.manga.title)
            }

            val consolidatedItems = if (matchingIndex == -1) {
                current.consolidatedItems + ConsolidatedSearchResult(
                    key = normalizeTitle(candidate.manga.title),
                    selected = candidate,
                    candidates = listOf(candidate),
                )
            } else {
                current.consolidatedItems.toMutableList().apply {
                    val currentResult = this[matchingIndex]
                    val candidates = currentResult.candidates + candidate
                    val selected = candidates.maxWithOrNull(
                        compareBy<ConsolidatedSearchCandidate> { it.chapterCount }
                            .thenBy { it.matchScore },
                    ) ?: currentResult.selected
                    set(
                        matchingIndex,
                        currentResult.copy(
                            selected = selected,
                            candidates = candidates,
                        ),
                    )
                }
            }

            current.copy(
                consolidatedItems = sortConsolidatedItems(consolidatedItems),
            )
        }
    }

    fun setConsolidatedSource(workKey: String, sourceId: Long) {
        mutableState.update { current ->
            val items = current.consolidatedItems.map { result ->
                if (result.key != workKey) return@map result
                result.candidates
                    .firstOrNull { it.source.id == sourceId }
                    ?.let { result.copy(selected = it) }
                    ?: result
            }
            current.copy(consolidatedItems = sortConsolidatedItems(items))
        }
    }

    private fun sortConsolidatedItems(items: List<ConsolidatedSearchResult>) = items.sortedWith(
        compareByDescending<ConsolidatedSearchResult> { it.bestMatchScore }
            .thenByDescending { it.chapterCount }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.manga.title },
    )

    private fun List<Manga>.bestSearchMatch(query: String): SearchMatch? {
        if (isEmpty()) return null

        val normalizedQuery = normalizeTitle(query)
        return maxByOrNull { candidate ->
            normalizedLevenshtein.similarity(normalizedQuery, normalizeTitle(candidate.title))
        }?.let { manga ->
            SearchMatch(
                manga = manga,
                score = normalizedLevenshtein.similarity(normalizedQuery, normalizeTitle(manga.title)),
            )
        }
    }

    private fun sameWork(firstTitle: String, secondTitle: String): Boolean {
        val first = normalizeTitle(firstTitle)
        val second = normalizeTitle(secondTitle)
        return first == second || normalizedLevenshtein.similarity(first, second) >= CONSOLIDATED_TITLE_THRESHOLD
    }

    private suspend fun getChapterCount(source: Source, manga: Manga): Int {
        return try {
            withContext(coroutineDispatcher) {
                source.getMangaUpdate(
                    manga = manga.toSManga(),
                    chapters = emptyList(),
                    fetchDetails = false,
                    fetchChapters = true,
                ).chapters.size
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            0
        }
    }

    private fun normalizeTitle(title: String): String {
        return title
            .lowercase(Locale.getDefault())
            .replace(Regex("[^\\p{L}0-9]+"), " ")
            .trim()
            .replace(Regex(" +"), " ")
    }

    private data class SearchMatch(
        val manga: Manga,
        val score: Double,
    )

    fun setMigrateDialog(currentId: Long, target: Manga) {
        screenModelScope.launchIO {
            val current = getManga.await(currentId) ?: return@launchIO
            mutableState.update { it.copy(dialog = Dialog.Migrate(target, current)) }
        }
    }

    fun clearDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    @Immutable
    data class State(
        val from: Manga? = null,
        val searchQuery: String? = null,
        val sourceFilter: SourceFilter = SourceFilter.All,
        val searchMode: GlobalSearchMode = GlobalSearchMode.CONSOLIDATED,
        val onlyShowHasResults: Boolean = false,
        val items: Map<Source, SearchItemResult> = mapOf(),
        val consolidatedItems: List<ConsolidatedSearchResult> = emptyList(),
        val dialog: Dialog? = null,
    ) {
        val progress: Int = items.count { it.value !is SearchItemResult.Loading }
        val total: Int = items.size
        val filteredItems = items.filter { (_, result) -> result.isVisible(onlyShowHasResults) }
    }

    sealed interface Dialog {
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }

    companion object {
        private const val CONSOLIDATED_TITLE_THRESHOLD = 0.78
    }
}

enum class SourceFilter {
    All,
    PinnedOnly,
}

sealed interface SearchItemResult {
    data object Loading : SearchItemResult

    data class Error(
        val throwable: Throwable,
    ) : SearchItemResult

    data class Success(
        val result: List<Manga>,
        val chapterCount: Int = 0,
    ) : SearchItemResult {
        val isEmpty: Boolean
            get() = result.isEmpty()
    }

    fun isVisible(onlyShowHasResults: Boolean): Boolean {
        return !onlyShowHasResults || (this is Success && !this.isEmpty)
    }
}

data class ConsolidatedSearchResult(
    val key: String,
    val selected: ConsolidatedSearchCandidate,
    val candidates: List<ConsolidatedSearchCandidate>,
) {
    val manga: Manga get() = selected.manga
    val source: Source get() = selected.source
    val chapterCount: Int get() = selected.chapterCount
    val matchScore: Double get() = selected.matchScore
    val bestMatchScore: Double get() = candidates.maxOfOrNull { it.matchScore } ?: matchScore
}

data class ConsolidatedSearchCandidate(
    val manga: Manga,
    val source: Source,
    val chapterCount: Int,
    val matchScore: Double,
) {
    fun isBetterThan(other: ConsolidatedSearchCandidate): Boolean {
        return chapterCount > other.chapterCount ||
            (chapterCount == other.chapterCount && matchScore > other.matchScore)
    }
}
