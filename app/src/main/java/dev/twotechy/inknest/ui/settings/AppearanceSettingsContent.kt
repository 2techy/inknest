package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AppearanceSettingsContent(state: SettingsUiState, actions: SettingsActions) {
    val preferences = state.preferences ?: return
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
        SettingsSection(stringResource(R.string.display_mode_heading)) {
            DisplayModeChoices(preferences.displayMode, !state.saving, actions.displayMode)
        }
        SettingsSection(stringResource(R.string.home_options_heading)) {
            SettingsToggle(
                stringResource(R.string.mini_stats),
                stringResource(R.string.mini_stats_description),
                preferences.showHomeStats,
                !state.saving,
                actions.showStats,
            )
            SettingsToggle(
                stringResource(R.string.home_pins),
                stringResource(R.string.home_pins_description),
                preferences.showHomePinnedApps,
                !state.saving,
                actions.showPins,
            )
        }
    }
}
