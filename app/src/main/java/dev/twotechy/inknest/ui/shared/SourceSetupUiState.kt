package dev.twotechy.inknest.ui.shared

import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsState
import dev.twotechy.inknest.model.AppInfo

internal data class SourceSetupUiState(
    val source: SourceSettingsState = SourceSettingsState(),
    val apps: List<AppInfo> = emptyList(),
    val pendingFolder: SourceFolder? = null,
    val saving: Boolean = false,
    val error: SourceSetupError? = null,
    val bookFolderSelection: Boolean = false,
)

// Each screen owns its actions/picker. Only persisted source data is shared by repositories.
