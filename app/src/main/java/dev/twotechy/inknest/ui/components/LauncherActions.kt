package dev.twotechy.inknest.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.LauncherModeController

internal data class LauncherActions(val request: () -> Unit, val openSettings: () -> Unit)

@Composable
internal fun rememberLauncherActions(controller: LauncherModeController): LauncherActions {
    val context = LocalContext.current
    val openSettings = {
        if (!controller.openHomeSettings()) {
            Toast.makeText(context, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK && !controller.isDefaultLauncher()) {
                Toast.makeText(context, R.string.home_role_not_set, Toast.LENGTH_SHORT).show()
            }
        }
    return LauncherActions(request = {
        try {
            val intent = controller.createRequestRoleIntent()
            if (intent == null) openSettings() else request.launch(intent)
        } catch (_: ActivityNotFoundException) {
            openSettings()
        } catch (_: SecurityException) {
            openSettings()
        }
    }, openSettings = openSettings)
}
