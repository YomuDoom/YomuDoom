package eu.kanade.tachiyomi.ui.recommendations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.recommendations.RecommendationsScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object RecommendationsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.label_recommendations),
                icon = rememberVectorPainter(Icons.Outlined.AutoAwesome),
            )
        }

    override suspend fun onReselect(navigator: Navigator) = Unit

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { RecommendationsScreenModel() }
        val state by screenModel.state.collectAsState()

        RecommendationsScreen(
            state = state,
            navigateUp = null,
            onRefresh = screenModel::refreshRecommendations,
            onClickItem = { navigator.push(MangaScreen(it.id)) },
            getManga = screenModel::getManga,
        )
    }
}
