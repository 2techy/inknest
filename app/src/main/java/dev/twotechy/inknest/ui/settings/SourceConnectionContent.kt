package dev.twotechy.inknest.ui.settings

import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceSettingsState
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.SourceAccessState
import dev.twotechy.inknest.ui.theme.InkSpacing

internal data class SourceConnectionUiState(
    val source: SourceSettingsState,
    val sourceApps: List<AppInfo>,
    val saving: Boolean,
)

@Composable
internal fun SourceConnectionContent(
    state: SourceConnectionUiState,
    onSelectApp: (String) -> Unit,
    onSelectFolder: (SourceFolder) -> Unit,
) {
    val source = state.source
    Column {
        SettingsValue(stringResource(R.string.source_status), sourceStatusLabel(source.status))
        if (!source.available) {
            Text(
                stringResource(R.string.integration_unavailable),
                modifier = Modifier.padding(vertical = InkSpacing.md),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SourceAppChoices(state, onSelectApp)
        SourceFolder.entries.forEach { folder ->
            SourceFolderRow(state, folder, onSelectFolder)
        }
    }
}

@Composable
private fun SourceFolderRow(
    state: SourceConnectionUiState,
    folder: SourceFolder,
    onSelectFolder: (SourceFolder) -> Unit,
) {
    val source = state.source
    val isKOReader = folder == SourceFolder.KOReader
    val path = if (isKOReader) source.koreaderFolder else source.booksFolder
    val access = if (isKOReader) source.koreaderAccess else source.booksAccess
    val label =
        path?.let(::sourceFolderLabel) ?: stringResource(R.string.folder_not_selected)
    val detail = sourceAccessError(access)?.let { stringResource(it) }
    SettingsRow(
        title = stringResource(
            if (isKOReader) R.string.koreader_folder else R.string.books_folder,
        ),
        description = if (detail == null) label else "$label · $detail",
        enabled = source.available && !state.saving,
        onClick = { onSelectFolder(folder) },
    )
}

private fun sourceFolderLabel(path: String): String = try {
    if (path.startsWith(
            "content://",
        )
    ) {
        DocumentsContract.getTreeDocumentId(Uri.parse(path)).substringAfter(':').ifEmpty {
            "/"
        }
    } else {
        path
    }
} catch (_: IllegalArgumentException) {
    path
}

private fun sourceAccessError(access: SourceAccessState?): Int? = when (access) {
    SourceAccessState.PermissionLost -> R.string.source_permission_lost

    SourceAccessState.InvalidFolder, SourceAccessState.Unsupported,
    SourceAccessState.ProviderError,
    -> R.string.source_error

    else -> null
}

@Composable
private fun SourceAppChoices(state: SourceConnectionUiState, onSelectApp: (String) -> Unit) {
    val onlyApp = state.sourceApps.singleOrNull()
    if (onlyApp != null && state.source.sourcePackage == onlyApp.componentId.substringBefore('/')) {
        SettingsValue(stringResource(R.string.source_app_title), onlyApp.label)
    } else if (state.sourceApps.isEmpty()) {
        Text(
            stringResource(R.string.source_app_missing),
            modifier = Modifier.padding(vertical = InkSpacing.sm),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        state.sourceApps.forEach { app ->
            val packageName = app.componentId.substringBefore('/')
            SettingsChoice(
                app.label,
                state.source.sourcePackage == packageName,
                !state.saving,
            ) { onSelectApp(packageName) }
        }
    }
}
