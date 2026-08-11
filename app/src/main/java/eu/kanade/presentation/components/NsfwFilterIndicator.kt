package eu.kanade.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun NsfwFilterIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(text = stringResource(MR.strings.ext_nsfw_short)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = stringResource(MR.strings.pref_category_nsfw_content),
            )
        },
    )
}
