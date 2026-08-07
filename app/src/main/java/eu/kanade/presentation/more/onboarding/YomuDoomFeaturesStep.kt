package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class YomuDoomFeaturesStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val features = listOf(
            stringResource(MR.strings.onboarding_yomudoom_feature_fast_startup_title) to
                stringResource(MR.strings.onboarding_yomudoom_feature_fast_startup_description),
            stringResource(MR.strings.onboarding_yomudoom_feature_search_title) to
                stringResource(MR.strings.onboarding_yomudoom_feature_search_description),
            stringResource(MR.strings.onboarding_yomudoom_feature_similar_title) to
                stringResource(MR.strings.onboarding_yomudoom_feature_similar_description),
            stringResource(MR.strings.onboarding_yomudoom_feature_recommendations_title) to
                stringResource(MR.strings.onboarding_yomudoom_feature_recommendations_description),
            stringResource(MR.strings.onboarding_yomudoom_feature_autoscroll_title) to
                stringResource(MR.strings.onboarding_yomudoom_feature_autoscroll_description),
            stringResource(MR.strings.onboarding_yomudoom_feature_library_sort_title) to
                stringResource(MR.strings.onboarding_yomudoom_feature_library_sort_description),
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(MR.strings.onboarding_yomudoom_features_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(MR.strings.onboarding_yomudoom_features_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            features.forEach { (title, description) ->
                YomuDoomFeatureCard(
                    title = title,
                    description = description,
                )
            }
        }
    }
}

@Composable
private fun YomuDoomFeatureCard(
    title: String,
    description: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
