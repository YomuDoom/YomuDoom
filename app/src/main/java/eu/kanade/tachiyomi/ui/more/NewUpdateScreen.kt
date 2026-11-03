package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.NewUpdateScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.MR

class NewUpdateScreen(
    private val versionName: String,
    private val changelogInfo: String,
    private val releaseLink: String,
    private val downloadLink: String,
    private val preview: Boolean = false,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val changelogInfoNoChecksum = remember {
            changelogInfo.replace("""---(\R|.)*Checksums(\R|.)*""".toRegex(), "")
        }

        val downloadState by AppUpdateDownloadJob.downloadState.collectAsState()
        var isStartingDownload by remember { mutableStateOf(false) }

        LaunchedEffect(downloadState) {
            when (val state = downloadState) {
                is AppUpdateDownloadJob.DownloadState.Completed -> {
                    AppUpdateDownloadJob.installApk(context, state.uri)
                    AppUpdateDownloadJob.resetState()
                    navigator.pop()
                }
                is AppUpdateDownloadJob.DownloadState.Error -> {
                    context.toast(MR.strings.update_check_notification_download_error)
                    AppUpdateDownloadJob.resetState()
                    isStartingDownload = false
                }
                else -> {}
            }
        }

        val progress = when (val state = downloadState) {
            is AppUpdateDownloadJob.DownloadState.Progress -> state.percent
            is AppUpdateDownloadJob.DownloadState.Completed -> 100
            else -> if (isStartingDownload) -1 else null
        }

        NewUpdateScreen(
            versionName = versionName,
            changelogInfo = changelogInfoNoChecksum,
            onOpenInBrowser = { context.openInBrowser(releaseLink) },
            onRejectUpdate = {
                if (progress != null) {
                    AppUpdateDownloadJob.stop(context)
                }
                navigator.pop()
            },
            onAcceptUpdate = {
                if (!preview) {
                    isStartingDownload = true
                    AppUpdateDownloadJob.start(
                        context = context,
                        url = downloadLink,
                        title = versionName,
                    )
                } else {
                    navigator.pop()
                }
            },
            downloadProgress = progress,
        )
    }
}
