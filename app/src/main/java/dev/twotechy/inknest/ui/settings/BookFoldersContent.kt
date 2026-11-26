package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.BookFolderState
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.ui.components.ConfirmationDialog
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuItem
import dev.twotechy.inknest.ui.shared.SourceFolderActions
import dev.twotechy.inknest.ui.theme.InkSettingsTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun BookFoldersContent(
    state: SourceConnectionUiState,
    actions: SourceFolderActions,
    compact: Boolean = false,
    status: BookFolderStatus = BookFolderStatus(),
) {
    var removing by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { removing = null }
    val enabled = state.source.available && !state.saving
    Column(Modifier.padding(top = if (compact) InkSpacing.lg else 0.dp)) {
        SettingsSection(
            stringResource(R.string.book_folders),
            contentSpacing = if (compact) InkSpacing.xs else InkSettingsTokens.headingGap,
        ) {
            state.source.booksFolders.forEach { folder ->
                key(folder.uri) {
                    BookFolderRow(folder, enabled, actions.replaceBookFolder) { removing = it }
                }
            }
            status.errorMessage?.let { message ->
                Text(
                    stringResource(message),
                    Modifier.padding(vertical = InkSpacing.xs)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AddBookFolderRow(
                enabled = enabled,
                saving = status.saving,
                showEmptyDetails = !compact && state.source.booksFolders.isEmpty(),
            ) {
                actions.selectFolder(SourceFolder.Books)
            }
        }
    }
    removing?.let { uri ->
        ConfirmationDialog(
            title = stringResource(R.string.remove_book_folder),
            message = stringResource(
                R.string.remove_book_folder_description,
                sourceFolderLabel(uri),
            ),
            confirmLabel = stringResource(R.string.remove_book_folder),
            onConfirm = {
                removing = null
                actions.removeBookFolder(uri)
            },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun AddBookFolderRow(
    enabled: Boolean,
    saving: Boolean,
    showEmptyDetails: Boolean,
    onClick: () -> Unit,
) {
    Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        SettingsRow(
            title = stringResource(
                if (saving) R.string.book_folder_saving else R.string.add_book_folder,
            ),
            description = if (showEmptyDetails) {
                stringResource(
                    R.string.folder_not_selected,
                )
            } else {
                null
            },
            enabled = enabled,
            descriptionFormat = SettingsDescriptionFormat.FolderDetails,
            onClick = onClick,
        )
    }
}

@Composable
private fun BookFolderRow(
    folder: BookFolderState,
    enabled: Boolean,
    replace: (String) -> Unit,
    remove: (String) -> Unit,
) {
    var menuOpen by remember(enabled) { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { menuOpen = false }
    val path = sourceFolderLabel(folder.uri)
    Box {
        SettingsRow(
            path.substringAfterLast('/').ifEmpty { path },
            folderDescription(folder.uri, folder.access),
            enabled = enabled,
            descriptionFormat = SettingsDescriptionFormat.FolderDetails,
            onClick = { menuOpen = true },
        )
        if (menuOpen) {
            ContextMenu(onDismiss = { menuOpen = false }, alignToEnd = true) {
                ContextMenuItem(stringResource(R.string.reselect_book_folder)) {
                    menuOpen = false
                    replace(folder.uri)
                }
                ContextMenuItem(stringResource(R.string.remove_book_folder)) {
                    menuOpen = false
                    remove(folder.uri)
                }
            }
        }
    }
}
