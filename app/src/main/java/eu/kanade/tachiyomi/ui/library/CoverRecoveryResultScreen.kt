package eu.kanade.tachiyomi.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.manga.components.BaseMangaListItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CoverRecoveryResultScreen(
    private val titles: List<String> = emptyList(),
    private val ids: List<Long> = emptyList(),
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val topBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

        var resolvedManga by remember { mutableStateOf<List<Manga>>(emptyList()) }
        var standaloneTitles by remember { mutableStateOf<List<String>>(emptyList()) }
        var isLoaded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val cachedManga = CoverRecoveryHolder.lastRecoveredManga
            if (cachedManga.isNotEmpty()) {
                resolvedManga = cachedManga
                isLoaded = true
                return@LaunchedEffect
            }

            try {
                val allLibraryManga = Injekt.get<GetLibraryManga>().await().map { it.manga }
                val idSet = ids.toSet()
                val titleSet = titles.toSet()

                val matched = allLibraryManga.filter { it.id in idSet || it.title in titleSet }
                val matchedTitles = matched.map { it.title }.toSet()
                val remainingTitles = titles.filter { it !in matchedTitles }

                resolvedManga = matched
                standaloneTitles = remainingTitles
            } catch (_: Exception) {
                standaloneTitles = titles.ifEmpty { CoverRecoveryHolder.lastRecoveredTitles }
            }
            isLoaded = true
        }

        val totalCount = resolvedManga.size + standaloneTitles.size

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.cover_recovery_results_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            if (isLoaded && totalCount == 0) {
                EmptyScreen(
                    stringRes = MR.strings.cover_recovery_no_updates,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                FastScrollLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = contentPadding,
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MaterialTheme.padding.medium,
                                    vertical = MaterialTheme.padding.small,
                                ),
                        ) {
                            Text(
                                text = stringResource(MR.strings.cover_recovery_updated_count, totalCount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        HorizontalDivider()
                    }

                    items(
                        items = resolvedManga,
                        key = { it.id },
                    ) { manga ->
                        BaseMangaListItem(
                            manga = manga,
                            onClickItem = { navigator.push(MangaScreen(manga.id)) },
                            actions = {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(start = MaterialTheme.padding.small)
                                        .size(24.dp),
                                )
                            },
                        )
                    }

                    items(
                        items = standaloneTitles,
                    ) { title ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = MaterialTheme.padding.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
