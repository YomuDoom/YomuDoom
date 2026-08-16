package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.LogoHeader
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.updaterEnabled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var checkingForUpdates by remember { mutableStateOf(false) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(
                contentPadding = contentPadding,
            ) {
                item {
                    LogoHeader(
                        iconPadding = PaddingValues(vertical = 56.dp),
                    )
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.version),
                        subtitle = getVersionName(withBuildDate = true),
                        onPreferenceClick = {
                            val deviceInfo = CrashLogUtil(context).getDebugInfo()
                            context.copyToClipboard("Debug information", deviceInfo)
                        },
                    )
                }

                if (updaterEnabled) {
                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.check_for_updates),
                            widget = if (checkingForUpdates) {
                                {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            } else {
                                null
                            },
                            onPreferenceClick = if (checkingForUpdates) {
                                null
                            } else {
                                {
                                    scope.launch {
                                        checkingForUpdates = true
                                        try {
                                            when (
                                                val result = AppUpdateChecker().checkForUpdate(
                                                    context,
                                                    forceCheck = true,
                                                )
                                            ) {
                                                is GetApplicationRelease.Result.NewUpdate -> {
                                                    navigator.push(
                                                        NewUpdateScreen(
                                                            versionName = result.release.version,
                                                            changelogInfo = result.release.info,
                                                            releaseLink = result.release.releaseLink,
                                                            downloadLink = result.release.downloadLink,
                                                        ),
                                                    )
                                                }
                                                GetApplicationRelease.Result.NoNewUpdate -> {
                                                    context.toast(MR.strings.update_check_no_new_updates)
                                                }
                                                GetApplicationRelease.Result.OsTooOld -> {
                                                    context.toast(MR.strings.update_check_eol)
                                                }
                                            }
                                        } catch (error: Exception) {
                                            if (error is CancellationException) throw error
                                            context.toast(MR.strings.update_check_failed)
                                        } finally {
                                            checkingForUpdates = false
                                        }
                                    }
                                }
                            },
                        )
                    }
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.licenses),
                        onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                    )
                }
            }
        }
    }

    fun getVersionName(withBuildDate: Boolean): String {
        return "YomuDoom v${BuildConfig.VERSION_NAME.substringBefore('-')}"
    }
}
