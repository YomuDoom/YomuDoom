package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun AutoScrollDialog(
    speedDpPerSecond: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var speed by remember(speedDpPerSecond) { mutableFloatStateOf(speedDpPerSecond.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.auto_scroll)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        MR.strings.auto_scroll_speed_value,
                        speed.toInt(),
                    ),
                )
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 10f..300f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(speed.toInt()) }) {
                Text(text = stringResource(MR.strings.action_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
