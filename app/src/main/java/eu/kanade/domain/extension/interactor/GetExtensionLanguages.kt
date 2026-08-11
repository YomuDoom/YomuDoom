package eu.kanade.domain.extension.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

class GetExtensionLanguages(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {
    fun subscribe(): Flow<List<String>> {
        return combine(
            extensionManager.isInitialized,
            preferences.enabledLanguages.changes(),
            preferences.showNsfwSource.changes(),
            extensionManager.availableExtensionsFlow,
        ) { initialized, enabledLanguage, showNsfwSources, availableExtensions ->
            if (!initialized) return@combine null

            availableExtensions
                .filter { showNsfwSources || !it.isNsfw }
                .flatMap { ext ->
                    ext.sources.map { it.lang }
                }
                .distinct()
                .sortedWith(
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }.filterNotNull()
    }
}
