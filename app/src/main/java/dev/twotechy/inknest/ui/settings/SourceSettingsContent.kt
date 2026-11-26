package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceStatus
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.shared.SourceFolderActions
import java.text.DateFormat
import java.util.Date

internal fun LazyListScope.readingDataSettings(state: SettingsUiState, actions: SettingsActions) {
    val source = state.source
    val connection = SourceConnectionUiState(source, state.sourceApps, state.saving)
    item {
        SettingsSection(stringResource(R.string.reading_source_heading)) {
            SourceConnectionContent(connection, actions.sourceApp) {
                actions.connect(SourceFolder.KOReader)
            }
        }
    }
    item {
        BookFoldersContent(
            connection,
            SourceFolderActions(
                actions.connect,
                actions.replaceBookFolder,
                actions.removeBookFolder,
            ),
        )
    }
    item {
        SettingsCommand(
            stringResource(R.string.disconnect_source),
            source.available && !state.saving &&
                (source.koreaderFolder != null || source.booksFolders.isNotEmpty()),
            actions.disconnect,
        )
    }
    item {
        SettingsSection(stringResource(R.string.reading_cache_heading)) {
            DataSettingsContent(state, actions)
        }
    }
}

@Composable
private fun DataSettingsContent(state: SettingsUiState, actions: SettingsActions) {
    val source = state.source
    val locale = currentUiLocale()
    Column {
        SettingsValue(
            stringResource(R.string.last_updated),
            source.lastUpdated?.let {
                DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,
                    DateFormat.SHORT,
                    locale,
                ).format(Date(it))
            } ?: stringResource(R.string.unknown_value),
        )
        SettingsValue(
            stringResource(R.string.cache_size),
            source.cacheBytes?.let {
                stringResource(R.string.cache_bytes, it)
            } ?: stringResource(R.string.unknown_value),
        )
        SettingsCommand(
            stringResource(if (source.refreshing) R.string.refreshing else R.string.refresh_source),
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
