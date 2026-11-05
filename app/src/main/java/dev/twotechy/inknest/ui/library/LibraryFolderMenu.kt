package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuItem
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.settings.sourceFolderLabel
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun LibraryFolderMenu(state: LibraryUiState, select: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.options.folderUri
    val label = selected?.let(::sourceFolderLabel) ?: stringResource(R.string.all_book_folders)
    Box {
        HeaderIconButton(R.drawable.ic_book_folders, label) { expanded = true }
        if (expanded) {
            ContextMenu(onDismiss = { expanded = false }) {
                Text(
                    stringResource(R.string.book_folders),
                    style = MaterialTheme.typography.labelLarge,
                )
                val options = listOf<String?>(null) + state.folders.map { it.uri }
                options.forEach { uri ->
                    val text = uri?.let(::sourceFolderLabel)
                        ?: stringResource(R.string.all_book_folders)
                    ContextMenuItem(
                        if (uri == selected) {
                            stringResource(R.string.library_selected_option, text)
                        } else {
                            text
                        },
                    ) {
                        expanded = false
                        select(uri)
                    }
                }
            }
        }
    }
}
