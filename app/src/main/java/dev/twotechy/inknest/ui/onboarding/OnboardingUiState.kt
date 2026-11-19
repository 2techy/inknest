package dev.twotechy.inknest.ui.onboarding

import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsState
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.settings.SourceConnectionUiState

internal data class OnboardingUiState(
    val step: Int = 0,
    val mode: DisplayMode = DisplayMode.EInk,
    val saving: Boolean = false,
    val saveError: Boolean = false,
    val preferenceError: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val connection: SourceConnectionUiState =
        SourceConnectionUiState(SourceSettingsState(), emptyList(), false),
    val sourceError: Boolean = false,
    val folderConflict: Boolean = false,
    val preferences: AppPreferences? = null,
    val pendingFolder: SourceFolder? = null,
    val finished: Boolean = false,
) {
    val errorMessage: Int?
        get() = when {
            folderConflict -> R.string.book_folder_conflict
            sourceError -> R.string.source_action_error
            saveError -> R.string.setup_save_error
            preferenceError -> R.string.preferences_save_error
            else -> null
        }
}
