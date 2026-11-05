package dev.twotechy.inknest.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Keep platform picker lifetime rules identical in Settings and onboarding.
@Composable
internal fun SourceFolderSelection(
    viewModel: SettingsViewModel,
    screenKey: Any,
    enabled: Boolean = true,
) {
    val pending by viewModel.pendingFolder.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    DisposableEffect(viewModel, screenKey, activity) {
        onDispose {
            if (activity?.isChangingConfigurations != true) viewModel.dismissFolder()
        }
    }
    SourceFolderPicker(
        folder = if (enabled) pending else null,
        onSelect = viewModel::acceptFolder,
        onDismiss = viewModel::dismissFolder,
    )
}
