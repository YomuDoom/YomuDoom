package eu.kanade.presentation.browse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.ConsolidatedSearchCandidate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchResultItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    action: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.extraSmall,
                )
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = subtitle)
            }
            action()
            IconButton(onClick = onClick) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
        content()
    }
}

@Composable
fun ConsolidatedSourceMenu(
    candidates: List<ConsolidatedSearchCandidate>,
    selectedSourceId: Long,
    onSelectSource: (Long) -> Unit,
) {
    if (candidates.size <= 1) return

    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(MR.strings.change_source),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            candidates
                .sortedWith(
                    compareByDescending<ConsolidatedSearchCandidate> {
                        it.chapterCount
                    }.thenBy { it.source.name },
                )
                .forEach { candidate ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${candidate.source.name} (${candidate.source.lang.uppercase()}) · " +
                                    "${candidate.chapterCount} " +
                                    stringResource(MR.strings.chapters).lowercase(),
                            )
                        },
                        onClick = {
                            onSelectSource(candidate.source.id)
                            expanded = false
                        },
                        trailingIcon = {
                            if (candidate.source.id == selectedSourceId) {
                                Icon(Icons.Outlined.Check, contentDescription = null)
                            }
                        },
                    )
                }
        }
    }
}

@Composable
fun GlobalSearchLoadingResultItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.medium),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
fun GlobalSearchErrorResultItem(message: String?) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Outlined.Error, contentDescription = null)
        Spacer(Modifier.height(4.dp))
        Text(
            text = message ?: stringResource(MR.strings.unknown_error),
            textAlign = TextAlign.Center,
        )
    }
}
