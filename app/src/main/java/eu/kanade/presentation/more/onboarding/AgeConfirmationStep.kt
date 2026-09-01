package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AgeConfirmationStep : OnboardingStep {

    private val showNsfwSource = Injekt.get<SourcePreferences>().showNsfwSource

    private var selectedValue: Boolean? by mutableStateOf(
        showNsfwSource.get().takeIf { showNsfwSource.isSet() },
    )

    override val isComplete: Boolean
        get() = selectedValue != null

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .selectableGroup(),
        ) {
            Text(
                text = stringResource(MR.strings.onboarding_age_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(MR.strings.onboarding_age_question),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = MaterialTheme.padding.medium),
            )
            Text(
                text = stringResource(MR.strings.onboarding_age_description),
                modifier = Modifier.padding(
                    top = MaterialTheme.padding.small,
                    bottom = MaterialTheme.padding.medium,
                ),
            )

            AgeOption(
                selected = selectedValue == true,
                text = stringResource(MR.strings.onboarding_age_adult),
                onClick = { select(true) },
            )
            AgeOption(
                selected = selectedValue == false,
                text = stringResource(MR.strings.onboarding_age_minor),
                onClick = { select(false) },
            )

            Text(
                text = stringResource(MR.strings.onboarding_age_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = MaterialTheme.padding.medium)
                    .secondaryItemAlpha(),
            )
        }
    }

    private fun select(value: Boolean) {
        selectedValue = value
        showNsfwSource.set(value)
    }
}

@Composable
private fun AgeOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        content = { Text(text) },
    )
}
