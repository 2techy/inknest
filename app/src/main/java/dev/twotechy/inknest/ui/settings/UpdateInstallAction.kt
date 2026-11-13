package dev.twotechy.inknest.ui.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppUpdateState
import dev.twotechy.inknest.platform.AppUpdateInstaller
import dev.twotechy.inknest.platform.UpdateInstallResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun rememberUpdateSettingsActions(
    viewModel: SettingsViewModel,
    installer: AppUpdateInstaller?,
    page: SettingsPage,
    state: AppUpdateState,
): UpdateSettingsActions {
    val context = LocalContext.current
    return UpdateSettingsActions(
        check = viewModel::checkForUpdates,
        automatic = viewModel::setAutomaticUpdates,
        download = viewModel::downloadUpdate,
        cancel = viewModel::cancelUpdateDownload,
        install = rememberUpdateInstallAction(viewModel, installer, page),
        openLink = { openWebsite(context, it) },
        releasePage = {
            openWebsite(
                context,
                state.snapshot.release?.pageUrl
                    ?: "https://github.com/2techy/inknest/releases/latest",
            )
        },
    )
}

@Composable
internal fun rememberUpdateInstallAction(
    viewModel: SettingsViewModel,
    installer: AppUpdateInstaller?,
    page: SettingsPage,
): () -> Unit {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Job?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { pending?.cancel() }
    DisposableEffect(page) { onDispose { pending?.cancel() } }
    return {
        if (pending?.isActive != true && lifecycle.currentState == Lifecycle.State.RESUMED) {
            pending = scope.launch {
                val file = viewModel.prepareUpdateInstall()
                if (file != null && lifecycle.currentState == Lifecycle.State.RESUMED) {
                    val result = installer?.open(file) ?: UpdateInstallResult.Unavailable
                    val message = when (result) {
                        UpdateInstallResult.Opened -> null

                        UpdateInstallResult.PermissionRequired ->
                            R.string.updates_install_permission

                        UpdateInstallResult.Unavailable -> R.string.updates_install_unavailable
                    }
                    if (message != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
