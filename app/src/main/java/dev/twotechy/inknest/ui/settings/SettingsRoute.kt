package dev.twotechy.inknest.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.LauncherModeController
import dev.twotechy.inknest.ui.components.rememberDefaultLauncher
import dev.twotechy.inknest.ui.home.HomeViewModel

@Composable
internal fun SettingsRoute(
    homeViewModel: HomeViewModel,
    launcherModeController: LauncherModeController,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val previewMode by homeViewModel.previewMode.collectAsStateWithLifecycle()
    val isDefaultLauncher by rememberDefaultLauncher(launcherModeController)
    val openHomeSettings = {
        if (!launcherModeController.openHomeSettings()) {
            Toast.makeText(context, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
    val roleRequest =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK &&
                !launcherModeController.isDefaultLauncher()
            ) {
                Toast.makeText(context, R.string.home_role_not_set, Toast.LENGTH_SHORT).show()
            }
        }
    SettingsScreen(
        mode = previewMode,
        isDefaultLauncher = isDefaultLauncher,
        modifier = modifier,
        contentPadding = contentPadding,
        onMode = homeViewModel::onPreviewModeChanged,
        onRequestLauncherRole = {
            val intent = launcherModeController.createRequestRoleIntent()
            if (intent == null) {
                openHomeSettings()
            } else {
                try {
                    roleRequest.launch(intent)
                } catch (
                    _: android.content.ActivityNotFoundException,
                ) {
                    openHomeSettings()
                } catch (_: SecurityException) {
                    openHomeSettings()
                }
            }
        },
        onOpenAndroidSettings = openHomeSettings,
    )
}
