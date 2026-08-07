package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

val DownloadedOnlyBannerBackgroundColor
    @Composable get() = MaterialTheme.colorScheme.tertiary
val IncognitoModeBannerBackgroundColor
    @Composable get() = MaterialTheme.colorScheme.primary
val IndexingBannerBackgroundColor
    @Composable get() = MaterialTheme.colorScheme.secondary

@Composable
fun WarningBanner(
    textRes: StringResource,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(textRes),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error)
            .padding(16.dp),
        color = MaterialTheme.colorScheme.onError,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun AppStateBanners(
    downloadedOnlyMode: Boolean,
    incognitoMode: Boolean,
    indexing: Boolean,
    sourceLoadingProgress: Float? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val mainInsets = WindowInsets.statusBars
    val mainInsetsTop = mainInsets.getTop(density)
    SubcomposeLayout(modifier = modifier) { constraints ->
        val sourceLoadingPlaceable = subcompose(0) {
            AnimatedVisibility(
                visible = sourceLoadingProgress != null,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SourceLoadingBanner(
                    progress = sourceLoadingProgress ?: 0f,
                    modifier = Modifier.windowInsetsPadding(mainInsets),
                )
            }
        }.fastMap { it.measure(constraints) }
        val sourceLoadingHeight = sourceLoadingPlaceable.fastMaxBy { it.height }?.height ?: 0

        val indexingPlaceable = subcompose(1) {
            AnimatedVisibility(
                visible = indexing,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val top = (mainInsetsTop - sourceLoadingHeight).coerceAtLeast(0)
                IndexingDownloadBanner(modifier = Modifier.windowInsetsPadding(WindowInsets(top = top)))
            }
        }.fastMap { it.measure(constraints) }
        val indexingHeight = indexingPlaceable.fastMaxBy { it.height }?.height ?: 0

        val downloadedOnlyPlaceable = subcompose(2) {
            AnimatedVisibility(
                visible = downloadedOnlyMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val top = (mainInsetsTop - sourceLoadingHeight - indexingHeight).coerceAtLeast(0)
                DownloadedOnlyModeBanner(
                    modifier = Modifier.windowInsetsPadding(WindowInsets(top = top)),
                )
            }
        }.fastMap { it.measure(constraints) }
        val downloadedOnlyHeight = downloadedOnlyPlaceable.fastMaxBy { it.height }?.height ?: 0

        val incognitoPlaceable = subcompose(3) {
            AnimatedVisibility(
                visible = incognitoMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                val top = (mainInsetsTop - sourceLoadingHeight - indexingHeight - downloadedOnlyHeight).coerceAtLeast(0)
                IncognitoModeBanner(
                    modifier = Modifier.windowInsetsPadding(WindowInsets(top = top)),
                )
            }
        }.fastMap { it.measure(constraints) }
        val incognitoHeight = incognitoPlaceable.fastMaxBy { it.height }?.height ?: 0

        layout(constraints.maxWidth, sourceLoadingHeight + indexingHeight + downloadedOnlyHeight + incognitoHeight) {
            sourceLoadingPlaceable.fastForEach {
                it.place(0, 0)
            }
            indexingPlaceable.fastForEach {
                it.place(0, sourceLoadingHeight)
            }
            downloadedOnlyPlaceable.fastForEach {
                it.place(0, sourceLoadingHeight + indexingHeight)
            }
            incognitoPlaceable.fastForEach {
                it.place(0, sourceLoadingHeight + indexingHeight + downloadedOnlyHeight)
            }
        }
    }
}

@Composable
private fun SourceLoadingBanner(progress: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .background(IndexingBannerBackgroundColor)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(modifier),
    ) {
        Text(
            text = stringResource(MR.strings.loading),
            color = MaterialTheme.colorScheme.onSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSecondary,
            trackColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.24f),
        )
    }
}

@Composable
private fun DownloadedOnlyModeBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(MR.strings.label_downloaded_only),
        modifier = Modifier
            .background(DownloadedOnlyBannerBackgroundColor)
            .fillMaxWidth()
            .padding(4.dp)
            .then(modifier),
        color = MaterialTheme.colorScheme.onTertiary,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun IncognitoModeBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(MR.strings.pref_incognito_mode),
        modifier = Modifier
            .background(IncognitoModeBannerBackgroundColor)
            .fillMaxWidth()
            .padding(4.dp)
            .then(modifier),
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun IndexingDownloadBanner(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .background(color = IndexingBannerBackgroundColor)
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.Center,
    ) {
        var textHeight by remember { mutableStateOf(0.dp) }
        CircularProgressIndicator(
            modifier = Modifier.requiredSize(textHeight),
            color = MaterialTheme.colorScheme.onSecondary,
            strokeWidth = textHeight / 8,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(MR.strings.download_notifier_cache_renewal),
            color = MaterialTheme.colorScheme.onSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            onTextLayout = {
                with(density) {
                    textHeight = it.size.height.toDp()
                }
            },
        )
    }
}
