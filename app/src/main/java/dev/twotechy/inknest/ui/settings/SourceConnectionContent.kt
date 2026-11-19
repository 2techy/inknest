package dev.twotechy.inknest.ui.settings

import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.model.SourceAccessState
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun SourceConnectionContent(
    state: SourceConnectionUiState,
    actions: SourceConnectionActions,
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
        SourceAppChoices(state, actions.selectApp)
        SettingsRow(
            stringResource(R.string.koreader_folder),
            folderDescription(source.koreaderFolder, source.koreaderAccess),
            enabled = source.available && !state.saving,
            onClick = { actions.selectFolder(SourceFolder.KOReader) },
        )
        BookFoldersContent(state, actions)
    }
}

@Composable
internal fun folderDescription(path: String?, access: SourceAccessState?): String {
    val label = path?.let(::sourceFolderLabel) ?: stringResource(R.string.folder_not_selected)
    val detail = sourceAccessError(access)?.let { stringResource(it) }
    return if (detail == null) label else "$label · $detail"
}

internal fun sourceFolderLabel(path: String): String = try {
    if (path.startsWith(
            "content://",
        )
    ) {
        DocumentsContract.getTreeDocumentId(path.toUri()).substringAfter(':').ifEmpty {
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
