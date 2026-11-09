package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.theme.InkLayoutTokens

@Composable
internal fun AppearanceSettingsContent(state: SettingsUiState, actions: SettingsActions) {
    val preferences = state.preferences ?: return
    var choosingMode by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { choosingMode = false }
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap)) {
        SettingsSection(stringResource(R.string.settings_general_heading)) {
            SettingsRow(
                stringResource(R.string.language_title),
                stringResource(state.language.label()),
                onClick = actions.language,
            )
            SettingsRow(
                stringResource(R.string.display_mode_heading),
                stringResource(
                    if (preferences.displayMode == DisplayMode.EInk) {
                        R.string.eink_mode
                    } else {
                        R.string.standard_mode
                    },
                ),
                enabled = !state.saving,
                onClick = { choosingMode = true },
            )
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
                stringResource(R.string.home_heatmap),
                stringResource(R.string.home_heatmap_description),
                preferences.showHomeHeatmap,
                !state.saving,
                actions.showHeatmap,
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
    if (choosingMode) {
        SettingsSelectionDialog(
            stringResource(R.string.display_mode_heading),
            onDismiss = { choosingMode = false },
        ) {
            DisplayModeChoices(preferences.displayMode, !state.saving) {
                choosingMode = false
                actions.displayMode(it)
            }
        }
    }
}
