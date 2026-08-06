package eu.kanade.presentation.browse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.manga.components.DotSeparatorNoSpaceText
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.InstallDialogState
import eu.kanade.tachiyomi.ui.browse.extension.BulkActionDialogState
import eu.kanade.tachiyomi.ui.browse.extension.BulkActionType
import eu.kanade.tachiyomi.ui.browse.extension.BulkActionItem
import eu.kanade.tachiyomi.ui.browse.extension.InstallMode
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun ExtensionScreen(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onClickTrustAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow

    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.isEmpty -> {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.empty_screen
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                    actions = listOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.extensionStores,
                            icon = Icons.Outlined.Settings,
                            onClick = { navigator.push(ExtensionStoresScreen()) },
                        ),
                    ),
                )
            }
            else -> {
                ExtensionContent(
                    state = state,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onOpenWebView = onOpenWebView,
                    onInstallExtension = onInstallExtension,
                    onUninstallExtension = onUninstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onTrustExtension = onTrustExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                    onClickTrustAll = onClickTrustAll,
                )
            }
        }
    }
}

@Composable
private fun ExtensionContent(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onClickTrustAll: () -> Unit,
) {
    val context = LocalContext.current
    var trustState by remember { mutableStateOf<Extension.Untrusted?>(null) }
    val installGranted = rememberRequestPackageInstallsPermissionState(initialValue = true)

    FastScrollLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        if (!installGranted && state.installer?.requiresSystemPermission == true) {
            item(key = "extension-permissions-warning") {
                WarningBanner(
                    textRes = MR.strings.ext_permission_install_apps_warning,
                    modifier = Modifier.clickable {
                        context.launchRequestPackageInstallsPermission()
                    },
                )
            }
        }

        state.items.forEach { (header, items) ->
            item(
                contentType = "header",
                key = "extensionHeader-${header.hashCode()}",
            ) {
                when (header) {
                    is ExtensionUiModel.Header.Resource -> {
                        val hasUntrusted = items.any { it.extension is Extension.Untrusted }
                        val action: @Composable RowScope.() -> Unit =
                            if (header.textRes == MR.strings.ext_updates_pending) {
                                {
                                    Button(onClick = { onClickUpdateAll() }) {
                                        Text(
                                            text = stringResource(MR.strings.ext_update_all),
                                            style = LocalTextStyle.current.copy(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        )
                                    }
                                }
                            } else if (header.textRes == MR.strings.ext_installed && hasUntrusted) {
                                {
                                    Button(onClick = { onClickTrustAll() }) {
                                        Text(
                                            text = stringResource(MR.strings.ext_trust_all),
                                            style = LocalTextStyle.current.copy(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        )
                                    }
                                }
                            } else {
                                {}
                            }
                        ExtensionHeader(
                            textRes = header.textRes,
                            modifier = Modifier.animateItemFastScroll(),
                            action = action,
                        )
                    }
                    is ExtensionUiModel.Header.Text -> {
                        ExtensionHeader(
                            text = header.text,
                            modifier = Modifier.animateItemFastScroll(),
                        )
                    }
                }
            }

            items(
                items = items,
                contentType = { "item" },
                key = { item ->
                    when (item.extension) {
                        is Extension.Untrusted -> "extension-untrusted-${item.hashCode()}"
                        is Extension.Installed -> "extension-installed-${item.hashCode()}"
                        is Extension.Available -> "extension-available-${item.hashCode()}"
                    }
                },
            ) { item ->
                ExtensionItem(
                    modifier = Modifier.animateItemFastScroll(),
                    item = item,
                    onClickItem = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Installed -> onOpenExtension(it)
                            is Extension.Untrusted -> {
                                trustState = it
                            }
                        }
                    },
                    onLongClickItem = onLongClickItem,
                    onClickItemSecondaryAction = {
                        when (it) {
                            is Extension.Available -> onOpenWebView(it)
                            is Extension.Installed -> onOpenExtension(it)
                            else -> {}
                        }
                    },
                    onClickItemCancel = onClickItemCancel,
                    onClickItemAction = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Installed -> {
                                if (it.hasUpdate) {
                                    onUpdateExtension(it)
                                } else {
                                    onOpenExtension(it)
                                }
                            }
                            is Extension.Untrusted -> {
                                trustState = it
                            }
                        }
                    },
                )
            }
        }
    }
    if (trustState != null) {
        ExtensionTrustDialog(
            onClickConfirm = {
                onTrustExtension(trustState!!)
                trustState = null
            },
            onClickDismiss = {
                onUninstallExtension(trustState!!)
                trustState = null
            },
            onDismissRequest = {
                trustState = null
            },
        )
    }

}

@Composable
private fun ExtensionItem(
    item: ExtensionUiModel.Item,
    onClickItem: (Extension) -> Unit,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (extension, installStep) = item
    BaseBrowseItem(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val idle = installStep.isCompleted()
                if (!idle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.dp,
                    )
                }

                val padding by animateDpAsState(
                    targetValue = if (idle) 0.dp else 8.dp,
                    label = "iconPadding",
                )
                ExtensionIcon(
                    extension = extension,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(padding),
                )
            }
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemAction = onClickItemAction,
                onClickItemSecondaryAction = onClickItemSecondaryAction,
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExtensionItemContent(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        // Won't look good but it's not like we can ellipsize overflowing content
        FlowRow(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                var hasAlreadyShownAnElement by remember { mutableStateOf(false) }
                if (extension is Extension.Installed && extension.lang.isNotEmpty()) {
                    hasAlreadyShownAnElement = true
                    Text(
                        text = LocaleHelper.getSourceDisplayName(extension.lang, LocalContext.current),
                    )
                }

                if (extension.versionName.isNotEmpty()) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    hasAlreadyShownAnElement = true
                    Text(
                        text = extension.versionName,
                    )
                }

                val warning = when {
                    extension is Extension.Untrusted -> MR.strings.ext_untrusted
                    extension is Extension.Installed && extension.isObsolete -> MR.strings.ext_obsolete
                    extension.isNsfw -> MR.strings.ext_nsfw_short
                    else -> null
                }
                if (warning != null) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    hasAlreadyShownAnElement = true
                    Text(
                        text = stringResource(warning).uppercase(),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (extension is Extension.Installed && !extension.isShared) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    Text(
                        text = stringResource(MR.strings.ext_installer_private),
                    )
                }

                if (!installStep.isCompleted()) {
                    DotSeparatorNoSpaceText()
                    Text(
                        text = when (installStep) {
                            InstallStep.Pending -> stringResource(MR.strings.ext_pending)
                            InstallStep.Downloading -> stringResource(MR.strings.ext_downloading)
                            InstallStep.Installing -> stringResource(MR.strings.ext_installing)
                            else -> error("Must not show non-install process text")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        when {
            !isIdle -> {
                IconButton(onClick = { onClickItemCancel(extension) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.action_cancel),
                    )
                }
            }
            installStep == InstallStep.Error -> {
                IconButton(onClick = { onClickItemAction(extension) }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(MR.strings.action_retry),
                    )
                }
            }
            installStep == InstallStep.Idle -> {
                when (extension) {
                    is Extension.Installed -> {
                        IconButton(onClick = { onClickItemSecondaryAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(MR.strings.action_settings),
                            )
                        }

                        if (extension.hasUpdate) {
                            IconButton(onClick = { onClickItemAction(extension) }) {
                                Icon(
                                    imageVector = Icons.Outlined.GetApp,
                                    contentDescription = stringResource(MR.strings.ext_update),
                                )
                            }
                        }
                    }
                    is Extension.Untrusted -> {
                        IconButton(onClick = { onClickItemAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = stringResource(MR.strings.ext_trust),
                            )
                        }
                    }
                    is Extension.Available -> {
                        if (extension.sources.isNotEmpty()) {
                            IconButton(
                                onClick = { onClickItemSecondaryAction(extension) },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = stringResource(MR.strings.action_open_in_web_view),
                                )
                            }
                        }

                        IconButton(onClick = { onClickItemAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.GetApp,
                                contentDescription = stringResource(MR.strings.ext_install),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionHeader(
    textRes: StringResource,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    ExtensionHeader(
        text = stringResource(textRes),
        modifier = modifier,
        action = action,
    )
}

@Composable
private fun ExtensionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.header,
        )
        action()
    }
}

@Composable
private fun ExtensionTrustDialog(
    onClickConfirm: () -> Unit,
    onClickDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.untrusted_extension))
        },
        text = {
            Text(text = stringResource(MR.strings.untrusted_extension_message))
        },
        confirmButton = {
            TextButton(onClick = onClickConfirm) {
                Text(text = stringResource(MR.strings.ext_trust))
            }
        },
        dismissButton = {
            TextButton(onClick = onClickDismiss) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun ExtensionInstallDialog(
    state: InstallDialogState,
    onDismissRequest: () -> Unit,
    onClickRecommended: () -> Unit,
    onClickAll: () -> Unit,
    onToggleLanguage: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        title = { Text(text = stringResource(MR.strings.ext_install_extensions)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                Text(
                    text = stringResource(MR.strings.ext_install_select_mode),
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.mode == InstallMode.Recommended,
                        onClick = onClickRecommended,
                    )
                    Text(text = stringResource(MR.strings.ext_install_recommended))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.mode == InstallMode.All,
                        onClick = onClickAll,
                    )
                    Text(text = stringResource(MR.strings.ext_install_all))
                }

                if (state.mode == InstallMode.All) {
                    WarningBanner(
                        textRes = MR.strings.ext_install_all_message,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Text(
                    text = stringResource(MR.strings.ext_install_select_languages),
                    modifier = Modifier.padding(top = 16.dp),
                    fontWeight = FontWeight.Medium,
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                ) {
                    items(state.languages, key = { it }) { language ->
                        val selected = language in state.selectedLanguages
                        ListItem(
                            modifier = Modifier.clickable { onToggleLanguage(language) },
                            trailingContent = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            content = {
                                Text(text = LocaleHelper.getSourceDisplayName(language, context))
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(MR.strings.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun ExtensionBulkActionDialog(
    state: BulkActionDialogState,
    onDismissRequest: () -> Unit,
    onToggleItem: (String) -> Unit,
    onSelectAll: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    val displayItems = if (state.action == BulkActionType.Install) {
        state.items.sortedWith(
            compareBy<BulkActionItem> { it.installed }
                .thenBy { (it.extension as? Extension.Available)?.lang.orEmpty() }
                .thenBy { it.extension.name },
        )
    } else {
        state.items
    }
    val title = when (state.action) {
        BulkActionType.Install -> MR.strings.ext_install_extensions
        BulkActionType.Update -> MR.strings.ext_update_all
        BulkActionType.Uninstall -> MR.strings.ext_uninstall_all
        BulkActionType.Trust -> MR.strings.ext_trust_all
    }

    val confirmText = when (state.action) {
        BulkActionType.Install -> MR.strings.ext_install
        BulkActionType.Update -> MR.strings.ext_update_all
        BulkActionType.Uninstall -> MR.strings.ext_uninstall
        BulkActionType.Trust -> MR.strings.ext_trust
    }
    val selectAllText = if (state.selectedCount == state.eligibleCount && state.eligibleCount > 0) {
        MR.strings.action_select_inverse
    } else {
        MR.strings.action_select_all
    }

    AlertDialog(
        title = { Text(text = stringResource(title)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(MR.strings.ext_install_preview_message),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onSelectAll) {
                        Text(text = stringResource(selectAllText))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .heightIn(max = 280.dp),
                ) {
                    items(displayItems, key = { it.extension.pkgName }) { item ->
                        ListItem(
                            leadingContent = {
                                Checkbox(
                                    checked = item.selected,
                                    onCheckedChange = if (item.eligible) { { onToggleItem(item.extension.pkgName) } } else null,
                                )
                            },
                            content = {
                                Text(text = item.extension.name)
                            },
                            supportingContent = {
                                Text(text = LocaleHelper.getSourceDisplayName(item.extension.lang, context))
                            },
                            trailingContent = {
                                Text(
                                    text = when (state.action) {
                                        BulkActionType.Install -> if (item.installed) {
                                            stringResource(MR.strings.ext_already_installed)
                                        } else if (item.selected) {
                                            stringResource(MR.strings.ext_install)
                                        } else {
                                            stringResource(MR.strings.not_selected)
                                        }
                                        BulkActionType.Update -> if (item.selected) {
                                            stringResource(MR.strings.ext_update)
                                        } else {
                                            stringResource(MR.strings.not_selected)
                                        }
                                        BulkActionType.Uninstall -> if (item.selected) {
                                            stringResource(MR.strings.ext_uninstall)
                                        } else {
                                            stringResource(MR.strings.not_selected)
                                        }
                                        BulkActionType.Trust -> if (item.selected) {
                                            stringResource(MR.strings.ext_trust)
                                        } else {
                                            stringResource(MR.strings.not_selected)
                                        }
                                    },
                                    color = if (item.selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(confirmText))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
