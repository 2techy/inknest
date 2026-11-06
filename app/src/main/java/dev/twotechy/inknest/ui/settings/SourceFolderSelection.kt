package dev.twotechy.inknest.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.twotechy.inknest.data.repository.SourceFolder

// Keep platform picker lifetime rules identical in Settings and onboarding.
@Composable
internal fun SourceFolderSelection(
    pending: SourceFolder?,
    onSelect: (SourceFolder, String, Int) -> Unit,
    onDismiss: () -> Unit,
    screenKey: Any,
) {
    val activity = LocalActivity.current
    DisposableEffect(screenKey, activity) {
        onDispose {
            if (activity?.isChangingConfigurations != true) onDismiss()
        }
    }
    SourceFolderPicker(
        folder = pending,
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}
