package eu.kanade.tachiyomi.ui.browse.extension

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.core.os.LocaleListCompat
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

    fun showInstallDialog() {
        val available = extensionManager.availableExtensionsFlow.value
        val languages = available
            .mapNotNull { it.lang.takeIf(String::isNotBlank) }
            .filterNot { it.equals("multi", ignoreCase = true) }
            .distinct()
            .sortedWith(LocaleHelper.comparator)
        val systemLanguage =
            LocaleListCompat.getAdjustedDefault()[0]?.language ?: java.util.Locale.getDefault().language
        val defaultSelected = languages
            .filter { it == systemLanguage || it.startsWith("$systemLanguage-") }
            .toSet()
            .ifEmpty {
                languages
                    .filter { it == "en" || it.startsWith("en-") }
                    .take(1)
                    .toSet()
            }
            .ifEmpty { languages.take(1).toSet() }

        mutableState.update { state ->
            state.copy(
                installDialog = InstallDialogState(
                    languages = languages,
                    selectedLanguages = defaultSelected,
                    mode = InstallMode.Recommended,
                    showLanguageSelector = false,
                ),
            )
        }
    }

    fun dismissInstallDialog() {
        mutableState.update { it.copy(installDialog = null) }
    }

    fun setInstallMode(mode: InstallMode) {
        mutableState.update { state ->
            state.copy(
                installDialog = state.installDialog?.copy(
                    mode = mode,
                    showLanguageSelector = mode == InstallMode.All,
                ),
            )
        }
    }

    fun showInstallLanguages() {
        mutableState.update { state ->
            state.copy(installDialog = state.installDialog?.copy(showLanguageSelector = true))
        }
    }

    fun toggleInstallLanguage(language: String) {
        mutableState.update { state ->
            val dialog = state.installDialog ?: return@update state
            val selected = dialog.selectedLanguages.toMutableSet().apply {
                if (!add(language)) remove(language)
            }
            state.copy(installDialog = dialog.copy(selectedLanguages = selected))
        }
    }

    fun confirmInstallDialog() {
        val dialog = state.value.installDialog ?: return
        val previewItems = buildInstallPreviewItems(dialog)

        dismissInstallDialog()

        mutableState.update { state ->
            state.copy(
                bulkActionDialog = BulkActionDialogState(
                    action = BulkActionType.Install,
                    items = previewItems,
                    installMode = dialog.mode,
                ),
            )
        }
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
                BulkActionType.Install -> {
                    dialog.items
                        .filter { it.eligible && it.selected }
                        .mapNotNull { it.extension as? Extension.Available }
                        .forEach { extension ->
                            extensionManager.installExtension(extension).collectToInstallUpdate(extension)
                        }
                }
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
            BulkActionType.Install -> buildInstallPreviewItems(
                InstallDialogState(
                    languages = emptyList(),
                    selectedLanguages = emptySet(),
                    mode = InstallMode.All,
                    showLanguageSelector = true,
                ),
            )
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

    private fun buildInstallPreviewItems(dialog: InstallDialogState): List<BulkActionItem> {
        val installedPackages = extensionManager.installedExtensionsFlow.value
            .asSequence()
            .map { it.pkgName }
            .toSet()

        val items = extensionManager.availableExtensionsFlow.value
            .asSequence()
            .filter { it.lang in dialog.selectedLanguages }
            .filter { dialog.mode == InstallMode.All || isRecommendedExtension(it) }
            .map { extension ->
                val installed = extension.pkgName in installedPackages
                BulkActionItem(
                    extension = extension,
                    installed = installed,
                    eligible = !installed,
                    selected = when (dialog.mode) {
                        InstallMode.Recommended -> !installed && isRecommendedExtension(extension)
                        InstallMode.All -> !installed
                    },
                )
            }
            .toList()

        return if (dialog.mode == InstallMode.Recommended) {
            items.sortedWith(
                compareBy<BulkActionItem> { it.installed }
                    .thenBy { (it.extension as Extension.Available).lang }
                    .thenBy { it.extension.name },
            )
        } else {
            items.sortedWith(
                compareBy<BulkActionItem> { (it.extension as Extension.Available).lang }
                    .thenBy { it.extension.name },
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
        val installDialog: InstallDialogState? = null,
        val bulkActionDialog: BulkActionDialogState? = null,
    ) {
        val isEmpty = items.isEmpty()
    }
}

@Immutable
data class InstallDialogState(
    val languages: List<String>,
    val selectedLanguages: Set<String>,
    val mode: InstallMode,
    val showLanguageSelector: Boolean,
)

@Immutable
data class InstallPreviewDialogState(
    val items: List<InstallPreviewItem>,
) {
    val pendingCount: Int = items.count { !it.installed }
}

@Immutable
data class InstallPreviewItem(
    val extension: Extension.Available,
    val installed: Boolean,
    val eligible: Boolean,
    val selected: Boolean,
)

@Immutable
data class BulkActionDialogState(
    val action: BulkActionType,
    val items: List<BulkActionItem>,
    val installMode: InstallMode? = null,
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
    Install,
    Uninstall,
    Trust,
}

enum class InstallMode {
    Recommended,
    All,
}

private fun isRecommendedExtension(extension: Extension.Available): Boolean {
    val normalizedName = normalizeInstallName(extension.name)
    return normalizedName in recommendedInstallNamesByLanguage[extension.lang].orEmpty()
}

private val recommendedInstallNamesByLanguage = mapOf(
    "en" to setOf(
        "mangadex",
        "comick",
        "bato",
    ),
    "pt" to setOf(
        "lermangas",
        "mangalivre",
        "mangahost",
    ),
    "es" to setOf(
        "mangaplus",
        "mangalib",
    ),
    "ja" to setOf(
        "comick",
        "mangadex",
    ),
    "ko" to setOf(
        "mangadex",
        "comick",
    ),
    "zh" to setOf(
        "mangadex",
        "comick",
    ),
    "fr" to setOf(
        "mangadex",
        "comick",
    ),
    "de" to setOf(
        "mangadex",
        "comick",
    ),
)

private fun normalizeInstallName(value: String): String {
    return value.lowercase()
        .replace(Regex("[^\\p{L}0-9]+"), " ")
        .trim()
        .replace(Regex(" +"), " ")
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
