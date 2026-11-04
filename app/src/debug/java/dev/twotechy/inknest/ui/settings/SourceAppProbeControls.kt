package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.ReadingOpenMode

internal data class SourceAppProbeState(
    val candidates: List<AppInfo>,
    val selectedComponent: String?,
    val targets: Int,
    val enabled: Boolean,
    val versionAvailable: Boolean,
    val confirmedA: Boolean,
    val confirmedB: Boolean,
    val mode: ReadingOpenMode,
)

@Composable
internal fun SourceAppProbeControls(
    state: SourceAppProbeState,
    select: (String) -> Unit,
    launchApp: () -> Unit,
    launchBook: (Int) -> Unit,
    confirmA: (Boolean) -> Unit,
    confirmB: (Boolean) -> Unit,
    record: (ReadingOpenMode) -> Unit,
) {
    Column {
        state.candidates.forEach { app ->
            SettingsChoice(
                app.label + " · " + app.componentId.substringBefore('/'),
                state.selectedComponent == app.componentId,
                state.enabled,
            ) { select(app.componentId.substringBefore('/')) }
        }
        if (state.candidates.isEmpty()) Text(stringResource(R.string.probe_reader_missing))
        val canOpen = state.enabled && state.selectedComponent != null
        SettingsCommand(stringResource(R.string.probe_launch_reader), canOpen, launchApp)
        repeat(state.targets) { index ->
            SettingsCommand(
                stringResource(
                    R.string.probe_open_book,
                    if (index ==
                        0
                    ) {
                        "A"
                    } else {
                        "B"
                    },
                ),
                canOpen,
            ) {
                launchBook(index)
            }
        }
        Text(stringResource(R.string.probe_open_warning))
        SettingsToggle(
            stringResource(R.string.probe_confirm_a),
            stringResource(R.string.probe_cold_warm),
            state.confirmedA,
            canOpen && state.targets == 2,
            confirmA,
        )
        SettingsToggle(
            stringResource(R.string.probe_confirm_b),
            stringResource(R.string.probe_cold_warm),
            state.confirmedB,
            canOpen && state.targets == 2,
            confirmB,
        )
        SettingsCommand(
            stringResource(R.string.probe_save_direct),
            state.enabled && state.confirmedA && state.confirmedB && state.targets == 2 &&
                state.versionAvailable,
        ) {
            record(ReadingOpenMode.DirectConfirmed)
        }
        SettingsCommand(
            stringResource(R.string.probe_save_fallback),
            state.enabled && state.versionAvailable,
        ) {
            record(ReadingOpenMode.SourceAppOnly)
        }
        val mode = when (state.mode) {
            ReadingOpenMode.Unverified -> R.string.probe_mode_unverified
            ReadingOpenMode.SourceAppOnly -> R.string.probe_mode_app_only
            ReadingOpenMode.DirectConfirmed -> R.string.probe_mode_direct
        }
        SettingsValue(stringResource(R.string.probe_open_mode), stringResource(mode))
    }
}
