package eu.kanade.tachiyomi.ui.browse.source.similar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.service.NsfwContentFilter
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.OnlineMangaFetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.State as ComposeState

class SimilarMangaScreenModel(
    private val mangaId: Long,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
) : StateScreenModel<SimilarMangaScreenModel.State>(State()) {

    private val mangaFetcher = OnlineMangaFetcher(networkToLocalManga)
    private val nsfwContentFilter = NsfwContentFilter(preferences = sourcePreferences)

    init {
        screenModelScope.launch {
            loadSimilarManga()
        }
        screenModelScope.launch {
            nsfwContentFilter.changes()
                .drop(1)
                .collectLatest {
                    mutableState.update { state -> state.copy(results = emptyList()) }
                }
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

    private suspend fun loadSimilarManga() {
        val reference = getManga.await(mangaId)
        if (reference == null) {
            mutableState.update { it.copy(isLoading = false, failedSources = 1) }
            return
        }
        if (!nsfwContentFilter.isMangaAllowed(reference)) {
            mutableState.update { it.copy(isLoading = false) }
            return
        }

        val sources = sourceManager.getOnlineSources()
            .filter { it.lang in sourcePreferences.enabledLanguages.get() }
            .filterNot { it.id.toString() in sourcePreferences.disabledSources.get() }
            .filter { nsfwContentFilter.isSourceAllowed(it.id) }
            .distinctBy { it.id }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        mutableState.update {
            it.copy(
                referenceTitle = reference.title,
                totalSources = sources.size,
                isLoading = sources.isNotEmpty(),
            )
        }

        if (sources.isEmpty()) return

        sources.map { source ->
            screenModelScope.async {
                try {
                    val results = findSimilarInSource(reference, source)
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

    private suspend fun findSimilarInSource(reference: Manga, source: HttpSource): List<Result> =
        mangaFetcher.fetch(source)
            .filterNot { isSameWork(reference, it) }
            .map { manga ->
                Result(
                    manga = manga,
                    sourceId = source.id,
                    sourceName = source.name,
                    language = source.lang,
                    score = SimilarMangaScorer.score(reference, manga),
                )
            }
            .sortedWith(compareByDescending<Result> { it.score }.thenBy { it.manga.title })
            .let { scored ->
                scored
                    .filter { it.score > 0 }
                    .ifEmpty { scored }
                    .take(MAX_RESULTS_PER_SOURCE)
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

    private fun isSameWork(reference: Manga, candidate: Manga): Boolean {
        return (reference.source == candidate.source && reference.url == candidate.url) ||
            SimilarMangaScorer.normalize(reference.title) == SimilarMangaScorer.normalize(candidate.title)
    }

    @Immutable
    data class Result(
        val manga: Manga,
        val sourceId: Long,
        val sourceName: String,
        val language: String,
        val score: Int,
    )

    @Immutable
    data class State(
        val referenceTitle: String? = null,
        val results: List<Result> = emptyList(),
        val completedSources: Int = 0,
        val totalSources: Int = 0,
        val failedSources: Int = 0,
        val isLoading: Boolean = true,
    )

    companion object {
        private const val MAX_RESULTS_PER_SOURCE = 8
        private val resultComparator = compareByDescending<Result> { it.score }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.manga.title }
    }
}
