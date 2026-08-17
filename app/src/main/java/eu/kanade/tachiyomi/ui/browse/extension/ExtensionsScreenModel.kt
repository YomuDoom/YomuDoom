package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

class ExtensionsScreenModel(
    private val preferences: SourcePreferences = Injekt.get(),
    basePreferences: BasePreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val getExtensions: GetExtensionsByType = Injekt.get(),
) : StateScreenModel<ExtensionsScreenModel.State>(State()) {

    private val currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

    init {
        val context = Injekt.get<Application>()
        val extensionMapper: (Map<String, InstallStep>) -> ((Extension) -> ExtensionUiModel.Item) = { map ->
            {
                ExtensionUiModel.Item(it, map[it.pkgName] ?: InstallStep.Idle)
            }
        }

        screenModelScope.launchIO {
            combine(
                state.map { it.searchQuery }
                    .distinctUntilChanged()
                    .debounce(0.25.seconds)
                    .map { searchQueryPredicate(it ?: "") },
                currentDownloads,
                getExtensions.subscribe(),
            ) { predicate, downloads, (_updates, _installed, _available, _untrusted) ->
                buildMap {
                    val updates = _updates.filter(predicate).map(extensionMapper(downloads))
                    if (updates.isNotEmpty()) {
                        put(ExtensionUiModel.Header.Resource(MR.strings.ext_updates_pending), updates)
                    }

                    val installed = _installed.filter(predicate).map(extensionMapper(downloads))
                    val untrusted = _untrusted.filter(predicate).map(extensionMapper(downloads))
                    if (installed.isNotEmpty() || untrusted.isNotEmpty()) {
                        put(ExtensionUiModel.Header.Resource(MR.strings.ext_installed), installed + untrusted)
                    }

                    val languagesWithExtensions = _available
                        .filter(predicate)
                        .groupBy { it.lang }
                        .toSortedMap(LocaleHelper.comparator)
                        .map { (lang, exts) ->
                            ExtensionUiModel.Header.Text(LocaleHelper.getSourceDisplayName(lang, context)) to
                                exts.map(extensionMapper(downloads))
                        }
                    if (languagesWithExtensions.isNotEmpty()) {
                        putAll(languagesWithExtensions)
                    }
                } to _untrusted.size
            }
                .collectLatest { (items, _) ->
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            items = items,
                        )
                    }
                }
        }

        screenModelScope.launchIO { findAvailableExtensions() }

        preferences.extensionUpdatesCount.changes()
            .onEach { mutableState.update { state -> state.copy(updates = it) } }
            .launchIn(screenModelScope)

        basePreferences.extensionInstaller.changes()
            .onEach { mutableState.update { state -> state.copy(installer = it) } }
            .launchIn(screenModelScope)
    }

    fun searchQueryPredicate(query: String): (Extension) -> Boolean {
        val subqueries = query.split(",")
            .map { it.trim() }
            .filterNot { it.isBlank() }

        if (subqueries.isEmpty()) return { true }

        return { extension ->
            subqueries.any { subquery ->
                if (extension.name.contains(subquery, ignoreCase = true)) return@any true

                when (extension) {
                    is Extension.Installed -> extension.sources.any { source ->
                        source.name.contains(subquery, ignoreCase = true) ||
                            (source as? HttpSource)?.getHomeUrl()?.contains(subquery, ignoreCase = true) == true ||
                            source.id == subquery.toLongOrNull()
                    }

                    is Extension.Available -> extension.sources.any {
                        it.name.contains(subquery, ignoreCase = true) ||
                            it.baseUrl.contains(subquery, ignoreCase = true) ||
                            it.id == subquery.toLongOrNull()
                    }

                    else -> false
                }
            }
        }
    }

    fun search(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    fun updateAllExtensions() {
        val extensions = extensionManager.installedExtensionsFlow.value.filter { it.hasUpdate }
        screenModelScope.launchIO {
            extensions.forEach { extension ->
                extensionManager.updateExtension(extension).collectToInstallUpdate(extension)
            }
        }
    }

    fun trustAllExtensions() {
        openBulkActionDialog(BulkActionType.Trust)
    }

    fun uninstallAllExtensions() {
        openBulkActionDialog(BulkActionType.Uninstall)
    }

    fun dismissBulkActionDialog() {
        mutableState.update { it.copy(bulkActionDialog = null) }
    }

    fun toggleBulkActionSelection(pkgName: String) {
        mutableState.update { state ->
            val dialog = state.bulkActionDialog ?: return@update state
            val updatedItems = dialog.items.map { item ->
                if (item.extension.pkgName == pkgName && item.eligible) {
                    item.copy(selected = !item.selected)
                } else {
                    item
                }
            }
            state.copy(bulkActionDialog = dialog.copy(items = updatedItems))
        }
    }

    fun selectAllBulkActionItems() {
        mutableState.update { state ->
            val dialog = state.bulkActionDialog ?: return@update state
            val selectableCount = dialog.items.count { it.eligible }
            val selectedCount = dialog.items.count { it.eligible && it.selected }
            val selectAll = selectedCount != selectableCount
            val updatedItems = dialog.items.map { item ->
                if (!item.eligible) {
                    item
                } else {
                    item.copy(selected = selectAll)
                }
            }
            state.copy(bulkActionDialog = dialog.copy(items = updatedItems))
        }
    }

    fun confirmBulkActionDialog() {
        val dialog = state.value.bulkActionDialog ?: return

        screenModelScope.launchIO {
            when (dialog.action) {
                BulkActionType.Uninstall -> {
                    dialog.items
                        .filter { it.eligible && it.selected }
                        .map { it.extension }
                        .forEach(extensionManager::uninstallExtension)
                }
                BulkActionType.Trust -> {
                    dialog.items
                        .filter { it.eligible && it.selected }
                        .mapNotNull { it.extension as? Extension.Untrusted }
                        .forEach { extension ->
                            extensionManager.trust(extension)
                        }
                }
            }
        }

        dismissBulkActionDialog()
    }

    private fun openBulkActionDialog(action: BulkActionType) {
        val items = when (action) {
            BulkActionType.Uninstall ->
                extensionManager.installedExtensionsFlow.value
                    .sortedWith(compareBy<Extension.Installed> { it.lang }.thenBy { it.name })
                    .map {
                        BulkActionItem(
                            extension = it,
                            installed = true,
                            eligible = true,
                            selected = false,
                        )
                    }
            BulkActionType.Trust ->
                extensionManager.untrustedExtensionsFlow.value
                    .sortedWith(compareBy<Extension.Untrusted> { it.lang }.thenBy { it.name })
                    .map {
                        BulkActionItem(
                            extension = it,
                            installed = false,
                            eligible = true,
                            selected = false,
                        )
                    }
        }

        if (items.isEmpty()) return

        mutableState.update { state ->
            state.copy(
                bulkActionDialog = BulkActionDialogState(
                    action = action,
                    items = items,
                ),
            )
        }
    }

    fun installExtension(extension: Extension.Available) {
        screenModelScope.launchIO {
            extensionManager.installExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun updateExtension(extension: Extension.Installed) {
        screenModelScope.launchIO {
            extensionManager.updateExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
        removeDownloadState(extension)
    }

    private fun addDownloadState(extension: Extension, installStep: InstallStep) {
        currentDownloads.update { it + Pair(extension.pkgName, installStep) }
    }

    private fun removeDownloadState(extension: Extension) {
        currentDownloads.update { it - extension.pkgName }
    }

    private suspend fun Flow<InstallStep>.collectToInstallUpdate(extension: Extension) =
        this
            .onEach { installStep -> addDownloadState(extension, installStep) }
            .takeWhile { installStep -> installStep != InstallStep.Installed }
            .onCompletion { removeDownloadState(extension) }
            .collect()

    fun uninstallExtension(extension: Extension) {
        extensionManager.uninstallExtension(extension)
    }

    fun findAvailableExtensions() {
        screenModelScope.launchIO {
            mutableState.update { it.copy(isRefreshing = true) }

            extensionManager.findAvailableExtensions()

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    fun trustExtension(extension: Extension.Untrusted) {
        screenModelScope.launch {
            extensionManager.trust(extension)
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val items: ItemGroups = mutableMapOf(),
        val updates: Int = 0,
        val installer: BasePreferences.ExtensionInstaller? = null,
        val searchQuery: String? = null,
        val bulkActionDialog: BulkActionDialogState? = null,
    ) {
        val isEmpty = items.isEmpty()
    }
}

@Immutable
data class BulkActionDialogState(
    val action: BulkActionType,
    val items: List<BulkActionItem>,
) {
    val selectedCount: Int = items.count { it.eligible && it.selected }
    val eligibleCount: Int = items.count { it.eligible }
}

@Immutable
data class BulkActionItem(
    val extension: Extension,
    val installed: Boolean,
    val eligible: Boolean,
    val selected: Boolean,
)

enum class BulkActionType {
    Uninstall,
    Trust,
}

typealias ItemGroups = Map<ExtensionUiModel.Header, List<ExtensionUiModel.Item>>

object ExtensionUiModel {
    sealed interface Header {
        data class Resource(val textRes: StringResource) : Header
        data class Text(val text: String) : Header
    }

    data class Item(
        val extension: Extension,
        val installStep: InstallStep,
    )
}
