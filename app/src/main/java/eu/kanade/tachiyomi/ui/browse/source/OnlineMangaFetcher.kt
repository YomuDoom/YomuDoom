package eu.kanade.tachiyomi.ui.browse.source

import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.Executors

class OnlineMangaFetcher(
    private val networkToLocalManga: NetworkToLocalManga,
) {

    private val dispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()

    suspend fun fetch(source: HttpSource): List<Manga> = withContext(dispatcher) {
        val catalog = buildList {
            addAll(fetchCatalogPage { source.getPopularManga(1) })
            if (source.supportsLatest) {
                addAll(fetchCatalogPage { source.getLatestUpdates(1) })
            }
        }
            .filter { it.url.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.url }
            .take(MAX_CATALOG_ITEMS)

        val detailedManga = catalog
            .take(MAX_DETAIL_REQUESTS)
            .map { candidate ->
                val manga = candidate.toDomainManga(source.id)
                try {
                    source.getMangaUpdate(
                        manga = manga.toSManga(),
                        chapters = emptyList(),
                        fetchDetails = true,
                        fetchChapters = false,
                    ).manga.toDomainManga(source.id)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    manga
                }
            }

        networkToLocalManga(detailedManga)
    }

    fun close() {
        dispatcher.close()
    }

    private suspend fun fetchCatalogPage(request: suspend () -> MangasPage): List<SManga> {
        return try {
            request().mangas
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    private companion object {
        const val MAX_CATALOG_ITEMS = 24
        const val MAX_DETAIL_REQUESTS = 12
    }
}
