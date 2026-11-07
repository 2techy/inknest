package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceStatus
import dev.twotechy.inknest.ui.components.currentUiLocale
import java.text.DateFormat
import java.util.Date

@Composable
internal fun SourceSettingsContent(state: SettingsUiState, actions: SettingsActions) {
    val source = state.source
    Column {
        SourceConnectionContent(
            SourceConnectionUiState(source, state.sourceApps, state.saving),
            SourceConnectionActions(
                actions.sourceApp,
                actions.connect,
                actions.replaceBookFolder,
                actions.removeBookFolder,
            ),
        )
        SettingsCommand(
            stringResource(R.string.disconnect_source),
            source.available && !state.saving &&
                (source.koreaderFolder != null || source.booksFolders.isNotEmpty()),
            actions.disconnect,
        )
    }
}

@Composable
internal fun DataSettingsContent(state: SettingsUiState, actions: SettingsActions) {
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
