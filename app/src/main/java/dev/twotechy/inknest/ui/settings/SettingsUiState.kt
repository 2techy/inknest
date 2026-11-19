package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsState
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.AppLanguage
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.AppUpdateState
import dev.twotechy.inknest.model.ThirdPartyLicense

internal data class SettingsUiState(
    val preferences: AppPreferences? = null,
    val language: AppLanguage = AppLanguage.System,
    val pendingFolder: SourceFolder? = null,
    val source: SourceSettingsState = SourceSettingsState(),
    val sourceApps: List<AppInfo> = emptyList(),
    val saving: Boolean = false,
    val error: SettingsError? = null,
    val acknowledgements: Map<ThirdPartyLicense, AcknowledgementsUiState> = emptyMap(),
    val appUpdate: AppUpdateState = AppUpdateState(),
    val releaseNotes: ReleaseNotesDocument = ReleaseNotesDocument(),
)
