package eu.kanade.tachiyomi.ui.browse.source.similar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

class SimilarMangaScreen(
    private val mangaId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SimilarMangaScreenModel(mangaId) }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                Column {
                    AppBar(
                        title = stringResource(MR.strings.similar_manga),
                        navigateUp = navigator::pop,
                    )
                    if (state.isLoading && state.totalSources > 0) {
                        LinearProgressIndicator(
                            progress = { state.completedSources / state.totalSources.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
        ) { contentPadding ->
            when {
                state.results.isNotEmpty() -> {
                    SimilarMangaContent(
                        state = state,
                        contentPadding = contentPadding,
                        onClickItem = { navigator.push(MangaScreen(it.id, true)) },
                        getManga = screenModel::getManga,
                    )
                }
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            CircularProgressIndicator()
                            if (state.totalSources > 0) {
                                Text(
                                    text = stringResource(
                                        MR.strings.similar_manga_progress,
                                        state.completedSources,
                                        state.totalSources,
                                    ),
                                )
                            }
                        }
                    }
                }
                else -> {
                    EmptyScreen(
                        stringRes = MR.strings.similar_manga_no_results,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarMangaContent(
    state: SimilarMangaScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Manga) -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
) {
    val groups = state.results.groupBy { it.sourceId }

    LazyColumn(
        contentPadding = contentPadding,
    ) {
        item {
            Text(
                text = state.referenceTitle.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.small,
                ),
            )
        }

        groups.forEach { (sourceId, results) ->
            val source = results.first()
            item(key = "similar-source-$sourceId") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.padding.medium,
                            vertical = MaterialTheme.padding.small,
                        ),
                ) {
                    Text(
                        text = buildString {
                            append(source.sourceName)
                            if (source.language.isNotBlank()) {
                                append(" (${source.language.uppercase()})")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            MR.strings.similar_manga_relevance,
                            results.maxOf { it.score },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item(key = "similar-items-$sourceId") {
                GlobalSearchCardRow(
                    titles = results.map { it.manga },
                    getManga = getManga,
                    onClick = onClickItem,
                    onLongClick = onClickItem,
                )
            }
        }
    }
}
