package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.BookFolderState
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.ui.components.ConfirmationDialog
import dev.twotechy.inknest.ui.shared.SourceFolderActions
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
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
    Column(Modifier.padding(top = InkSpacing.lg)) {
        SettingsSection(
            stringResource(R.string.book_folders),
            contentSpacing = if (compact) InkSpacing.xs else InkSettingsTokens.headingGap,
        ) {
            if (!compact && state.source.booksFolders.isEmpty()) {
                Text(
                    stringResource(R.string.folder_not_selected),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.source.booksFolders.forEach { folder ->
                BookFolderRow(folder, enabled, actions.replaceBookFolder) { removing = it }
            }
            status.errorMessage?.let { message ->
                Text(
                    stringResource(message),
                    Modifier.padding(vertical = InkSpacing.xs)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val addFolderLabel = stringResource(
                if (status.saving) R.string.book_folder_saving else R.string.add_book_folder,
            )
            val addFolder = { actions.selectFolder(SourceFolder.Books) }
            if (compact) {
                Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    SettingsRow(addFolderLabel, enabled = enabled, onClick = addFolder)
                }
            } else {
                SettingsCommand(addFolderLabel, enabled, addFolder)
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
private fun BookFolderRow(
    folder: BookFolderState,
    enabled: Boolean,
    replace: (String) -> Unit,
    remove: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            val path = sourceFolderLabel(folder.uri)
            SettingsRow(
                path.substringAfterLast('/').ifEmpty { path },
                folderDescription(folder.uri, folder.access),
                enabled = enabled,
                descriptionMaxLines = Int.MAX_VALUE,
                onClick = { replace(folder.uri) },
            )
        }
        Box(
            Modifier.size(InkHeaderTokens.touchTarget).clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { remove(folder.uri) },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_close),
                stringResource(R.string.remove_named_book_folder, sourceFolderLabel(folder.uri)),
                Modifier.size(InkHeaderTokens.iconSize),
            )
        }
    }
}
