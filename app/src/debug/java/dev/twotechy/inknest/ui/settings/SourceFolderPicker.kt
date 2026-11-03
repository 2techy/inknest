package dev.twotechy.inknest.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.ui.components.ConfirmationDialog

@Composable
internal fun SourceFolderPicker(
    folder: SourceFolder?,
    onSelect: (SourceFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    folder?.let { selected ->
        ConfirmationDialog(
            title = stringResource(R.string.choose_sample_folder),
            message = stringResource(R.string.sample_folder_explanation),
            confirmLabel = stringResource(R.string.use_sample_folder),
            onConfirm = { onSelect(selected) },
            onDismiss = onDismiss,
        )
    }
}
