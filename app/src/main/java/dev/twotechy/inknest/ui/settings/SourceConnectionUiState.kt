package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.data.repository.sources.SourceSettingsState
import dev.twotechy.inknest.model.AppInfo

internal data class SourceConnectionUiState(
    val source: SourceSettingsState,
    val sourceApps: List<AppInfo>,
    val saving: Boolean,
)
