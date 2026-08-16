package eu.kanade.presentation.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

fun Context.canRequestPackageInstallsCompat(): Boolean {
    if (packageManager.canRequestPackageInstalls()) return true

    val appOps = getSystemService<AppOpsManager>() ?: return false
    return runCatching {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                "android:request_install_packages",
                Process.myUid(),
                packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                "android:request_install_packages",
                Process.myUid(),
                packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
}

@Composable
fun rememberRequestPackageInstallsPermissionState(initialValue: Boolean = false): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var installGranted by remember {
        mutableStateOf(initialValue || context.canRequestPackageInstallsCompat())
    }

    LaunchedEffect(lifecycleOwner.lifecycle, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                installGranted = context.canRequestPackageInstallsCompat()
                delay(500)
            }
        }
    }

    return installGranted
}
