package dev.twotechy.inknest.ui.onboarding

import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.settings.SourceConnectionActions
import dev.twotechy.inknest.ui.settings.SourceConnectionUiState

internal data class OnboardingUiState(
    val step: Int,
    val mode: DisplayMode,
    val saving: Boolean,
    val saveError: Boolean,
    val preferenceError: Boolean,
    val isDefaultLauncher: Boolean,
    val connection: SourceConnectionUiState,
    val sourceError: Boolean,
    val folderConflict: Boolean,
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

internal data class OnboardingActions(
    val mode: (DisplayMode) -> Unit,
    val next: () -> Unit,
    val back: () -> Unit,
    val finish: () -> Unit,
    val requestLauncher: () -> Unit,
    val connection: SourceConnectionActions,
)
