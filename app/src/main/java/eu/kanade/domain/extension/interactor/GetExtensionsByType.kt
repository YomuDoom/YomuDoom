package eu.kanade.domain.extension.interactor

import eu.kanade.domain.extension.model.Extensions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class GetExtensionsByType(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<Extensions> {
        return combine(
            extensionManager.isInitialized,
            preferences.showNsfwSource.changes(),
            preferences.enabledLanguages.changes(),
        ) { initialized, showNsfwSources, enabledLanguages ->
            State(initialized, showNsfwSources, enabledLanguages)
        }
            .combine(extensionManager.installedExtensionsFlow) { state, installed ->
                state.copy(installed = installed)
            }
            .combine(extensionManager.untrustedExtensionsFlow) { state, untrusted ->
                state.copy(untrusted = untrusted)
            }
            .combine(extensionManager.availableExtensionsFlow) { state, available ->
                state.copy(available = available)
            }
            .map { state ->
                if (!state.initialized) return@map null

                val showNsfwSources = state.showNsfwSources
                val enabledLanguages = state.enabledLanguages
                val installedExtensions = state.installed
                val untrustedExtensions = state.untrusted
                val availableExtensions = state.available

                val (updates, installed) = installedExtensions
                    .filter { (showNsfwSources || !it.isNsfw) }
                    .sortedWith(
                        compareBy<Extension.Installed> { !it.isObsolete }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                    )
                    .partition { it.hasUpdate }

                val untrusted = untrustedExtensions
                    .filter { showNsfwSources || !it.isNsfw }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

                val available = availableExtensions
                    .filter { extension ->
                        installedExtensions.none { it.pkgName == extension.pkgName } &&
                            untrustedExtensions.none { it.pkgName == extension.pkgName } &&
                            (showNsfwSources || !extension.isNsfw)
                    }
                    .flatMap { ext ->
                        ext.sources.filter { it.lang in enabledLanguages }
                            .map {
                                ext.copy(
                                    name = it.name,
                                    lang = it.lang,
                                    pkgName = "${ext.pkgName}-${it.id}",
                                    sources = listOf(it),
                                )
                            }
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

                Extensions(updates, installed, available, untrusted)
            }
            .filterNotNull()
    }

    private data class State(
        val initialized: Boolean,
        val showNsfwSources: Boolean,
        val enabledLanguages: Set<String>,
        val installed: List<Extension.Installed> = emptyList(),
        val untrusted: List<Extension.Untrusted> = emptyList(),
        val available: List<Extension.Available> = emptyList(),
    )
}
