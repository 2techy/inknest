package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppInfo

internal data class SourceAppProbeState(
    val candidates: List<AppInfo>,
    val selectedComponent: String?,
    val targets: Int,
    val enabled: Boolean,
)

@Composable
internal fun SourceAppProbeControls(
    state: SourceAppProbeState,
    select: (String) -> Unit,
    launchApp: () -> Unit,
    launchBook: (Int) -> Unit,
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
    }
}
