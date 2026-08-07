package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.service.GlobalSearchMode
import eu.kanade.presentation.browse.components.ConsolidatedSourceMenu
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.ConsolidatedSearchResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchScreen(
    state: SearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    searchMode: GlobalSearchMode,
    consolidatedItems: List<ConsolidatedSearchResult>,
    onChangeSearchMode: (GlobalSearchMode) -> Unit,
    onChangeConsolidatedSource: (String, Long) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                showSearchMode = true,
                hideSourceFilter = searchMode == GlobalSearchMode.CONSOLIDATED,
                searchMode = searchMode,
                onChangeSearchMode = onChangeSearchMode,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
            searchMode = searchMode,
            consolidatedItems = consolidatedItems,
            onChangeConsolidatedSource = onChangeConsolidatedSource,
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<Source, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    searchMode: GlobalSearchMode = GlobalSearchMode.LEGACY,
    consolidatedItems: List<ConsolidatedSearchResult> = emptyList(),
    onChangeConsolidatedSource: (String, Long) -> Unit = { _, _ -> },
    fromSourceId: Long? = null,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        if (searchMode == GlobalSearchMode.CONSOLIDATED) {
            if (consolidatedItems.isEmpty()) {
                item {
                    if (items.values.any { it is SearchItemResult.Loading }) {
                        GlobalSearchLoadingResultItem()
                    } else {
                        Text(
                            text = stringResource(MR.strings.no_results_found),
                            modifier = Modifier.padding(MaterialTheme.padding.medium),
                        )
                    }
                }
            }

            consolidatedItems.forEach { result ->
                item(key = "consolidated-${result.manga.source}-${result.manga.url}") {
                    val chaptersLabel = stringResource(MR.strings.chapters).lowercase()
                    GlobalSearchResultItem(
                        title = result.manga.title,
                        subtitle = if (result.chapterCount > 0) {
                            "${result.chapterCount} $chaptersLabel"
                        } else {
                            ""
                        },
                        onClick = { onClickItem(result.manga) },
                        action = {
                            ConsolidatedSourceMenu(
                                candidates = result.candidates,
                                selectedSourceId = result.source.id,
                                onSelectSource = { sourceId ->
                                    onChangeConsolidatedSource(result.key, sourceId)
                                },
                            )
                        },
                        modifier = Modifier.animateItem(),
                    ) {
                        GlobalSearchCardRow(
                            titles = listOf(result.manga),
                            getManga = getManga,
                            onClick = onClickItem,
                            onLongClick = onLongClickItem,
                        )
                    }
                }
            }
        } else {
            items.forEach { (source, result) ->
                item(key = source.id) {
                    val chaptersLabel = stringResource(MR.strings.chapters).lowercase()
                    GlobalSearchResultItem(
                        title = fromSourceId?.let {
                            "▶ ${source.name}".takeIf { source.id == fromSourceId }
                        } ?: source.name,
                        subtitle = buildString {
                            append(LocaleHelper.getLocalizedDisplayName(source.lang))
                            if (result is SearchItemResult.Success && result.chapterCount > 0) {
                                append(" · ")
                                append(result.chapterCount)
                                append(' ')
                                append(chaptersLabel)
                            }
                        },
                        onClick = { onClickSource(source) },
                        modifier = Modifier.animateItem(),
                    ) {
                        when (result) {
                            SearchItemResult.Loading -> {
                                GlobalSearchLoadingResultItem()
                            }
                            is SearchItemResult.Success -> {
                                GlobalSearchCardRow(
                                    titles = result.result,
                                    getManga = getManga,
                                    onClick = onClickItem,
                                    onLongClick = onLongClickItem,
                                )
                            }
                            is SearchItemResult.Error -> {
                                GlobalSearchErrorResultItem(message = result.throwable.message)
                            }
                        }
                    }
                }
            }
        }
    }
}
