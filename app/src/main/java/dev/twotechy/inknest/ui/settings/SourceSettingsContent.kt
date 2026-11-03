package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceStatus
import dev.twotechy.inknest.ui.theme.InkSpacing
import java.text.DateFormat
import java.util.Date

@Composable
internal fun SourceSettingsContent(state: SettingsUiState, actions: SettingsActions) {
    val source = state.source
    Column {
        if (source.isSample) {
            Text(stringResource(R.string.sample_data), style = MaterialTheme.typography.labelSmall)
        }
        SettingsValue(stringResource(R.string.source_status), sourceStatusLabel(source.status))
        if (!source.available) {
            Text(
                stringResource(R.string.integration_unavailable),
                modifier = Modifier.padding(vertical = InkSpacing.md),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SourceFolder.entries.forEach { folder ->
            val isKOReader = folder == SourceFolder.KOReader
            val path = if (isKOReader) source.koreaderFolder else source.booksFolder
            SettingsRow(
                title = stringResource(
                    if (isKOReader) R.string.koreader_folder else R.string.books_folder,
                ),
                description = path ?: stringResource(R.string.folder_not_selected),
                enabled = source.available && !state.saving,
                onClick = { actions.connect(folder) },
            )
        }
        SettingsCommand(
            stringResource(R.string.disconnect_source),
            source.available && !state.saving &&
                (source.koreaderFolder != null || source.booksFolder != null),
            actions.disconnect,
        )
    }
}

@Composable
internal fun DataSettingsContent(state: SettingsUiState, actions: SettingsActions) {
    val source = state.source
    Column {
        SettingsValue(
            stringResource(R.string.refresh_status),
            stringResource(
                when {
                    source.refreshing -> R.string.refreshing
                    source.status == SourceStatus.Connected -> R.string.refresh_ready
                    else -> R.string.refresh_unavailable
                },
            ),
        )
        SettingsValue(
            stringResource(R.string.last_updated),
            source.lastUpdated?.let {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
            } ?: stringResource(R.string.unknown_value),
        )
        SettingsValue(
            stringResource(R.string.cache_size),
            source.cacheBytes?.let {
                stringResource(R.string.cache_bytes, it)
            } ?: stringResource(R.string.unknown_value),
        )
        SettingsCommand(
            stringResource(R.string.refresh_source),
            source.available && source.status == SourceStatus.Connected && !source.refreshing &&
                !state.saving,
            actions.refresh,
        )
        SettingsCommand(
            stringResource(R.string.clear_reading_cache),
            source.available && !state.saving && source.cacheBytes != null,
            actions.clearCache,
        )
    }
}
