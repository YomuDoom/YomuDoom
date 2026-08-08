package eu.kanade.presentation.recommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.recommendations.RecommendationsScreenModel
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun RecommendationsScreen(
    state: RecommendationsScreenModel.State,
    navigateUp: (() -> Unit)?,
    onRefresh: () -> Unit,
    onClickItem: (Manga) -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
) {
    Scaffold(
        topBar = {
            Column {
                AppBar(
                    title = stringResource(MR.strings.label_recommendations),
                    navigateUp = navigateUp,
                    actions = {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !state.isLoading,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(MR.strings.action_webview_refresh),
                            )
                        }
                    },
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
                RecommendationsContent(
                    state = state,
                    contentPadding = contentPadding,
                    onClickItem = onClickItem,
                    getManga = getManga,
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
                                    MR.strings.recommendations_progress,
                                    state.completedSources,
                                    state.totalSources,
                                ),
                            )
                        }
                    }
                }
            }
            state.profileSize == 0 -> {
                EmptyScreen(
                    stringRes = MR.strings.recommendations_no_history,
                    modifier = Modifier.padding(contentPadding),
                )
            }
            else -> {
                EmptyScreen(
                    stringRes = MR.strings.recommendations_no_results,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}

@Composable
private fun RecommendationsContent(
    state: RecommendationsScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Manga) -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
) {
    val groups = state.results.groupBy { it.sourceId }

    LazyColumn(contentPadding = contentPadding) {
        item {
            Text(
                text = stringResource(MR.strings.recommendations_based_on, state.profileSize),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.small,
                ),
            )
        }

        groups.forEach { (sourceId, results) ->
            val source = results.first()
            item(key = "recommendations-source-$sourceId") {
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
                            MR.strings.recommendations_relevance,
                            results.maxOf { it.score },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item(key = "recommendations-items-$sourceId") {
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
