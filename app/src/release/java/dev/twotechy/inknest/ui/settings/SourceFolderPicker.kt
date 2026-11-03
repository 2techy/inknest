package dev.twotechy.inknest.ui.settings

import androidx.compose.runtime.Composable
import dev.twotechy.inknest.data.repository.SourceFolder

// M2 will replace this boundary with the real SAF picker.
@Composable
internal fun SourceFolderPicker(
    folder: SourceFolder?,
    onSelect: (SourceFolder) -> Unit,
    onDismiss: () -> Unit,
) = Unit
