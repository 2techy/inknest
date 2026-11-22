package dev.twotechy.inknest.ui.onboarding

import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsState
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.model.SourceAccessState
import dev.twotechy.inknest.ui.settings.BookFolderStatus
import dev.twotechy.inknest.ui.settings.SourceConnectionUiState
import dev.twotechy.inknest.ui.shared.SourceSetupError

internal data class OnboardingUiState(
    val step: Int = 0,
    val mode: DisplayMode = DisplayMode.EInk,
    val saving: Boolean = false,
    val saveError: Boolean = false,
    val preferenceError: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val koreaderDetection: KoreaderDetection = KoreaderDetection.Checking,
    val connection: SourceConnectionUiState =
        SourceConnectionUiState(SourceSettingsState(), emptyList(), false),
    val sourceError: SourceSetupError? = null,
    val bookFolderSelection: Boolean = false,
    val preferences: AppPreferences? = null,
    val pendingFolder: SourceFolder? = null,
    val finished: Boolean = false,
) {
    val hasReadingFolder: Boolean
        get() = connection.source.koreaderAccess == SourceAccessState.Granted ||
            connection.source.booksFolders.any { it.access == SourceAccessState.Granted }

    private val sourceErrorMessage: Int?
        get() = when (sourceError) {
            SourceSetupError.FolderConflict -> R.string.book_folder_conflict
            SourceSetupError.Source -> R.string.source_action_error
            null -> null
        }

    val bookFolderStatus: BookFolderStatus
        get() = BookFolderStatus(
            saving = bookFolderSelection && sourceError == null,
            errorMessage = sourceErrorMessage.takeIf { bookFolderSelection },
        )

    val errorMessage: Int?
        get() = sourceErrorMessage.takeUnless { bookFolderSelection } ?: when {
            saveError -> R.string.setup_save_error
            preferenceError -> R.string.preferences_save_error
            else -> null
        }
}
