package dev.twotechy.inknest.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.ui.components.LauncherActions
import dev.twotechy.inknest.ui.settings.SettingsError
import dev.twotechy.inknest.ui.settings.SettingsViewModel
import dev.twotechy.inknest.ui.settings.SourceConnectionActions
import dev.twotechy.inknest.ui.settings.SourceConnectionUiState
import dev.twotechy.inknest.ui.settings.SourceFolderSelection

@Composable
internal fun OnboardingRoute(
    onboarding: OnboardingViewModel,
    settings: SettingsViewModel,
    isDefault: Boolean,
    launcher: LauncherActions,
    padding: PaddingValues,
) {
    val step by onboarding.step.collectAsStateWithLifecycle()
    val saving by onboarding.saving.collectAsStateWithLifecycle()
    val error by onboarding.error.collectAsStateWithLifecycle()
    val preferences by settings.uiState.collectAsStateWithLifecycle()
    SourceFolderSelection(settings, step, enabled = step == 1)
    BackHandler { if (!saving && !preferences.saving) onboarding.back() }
    OnboardingScreen(
        state = OnboardingUiState(
            step = step,
            mode = preferences.preferences?.displayMode ?: return,
            saving = saving || preferences.saving,
            saveError = error,
            preferenceError = preferences.error == SettingsError.Save,
            isDefaultLauncher = isDefault,
            connection = SourceConnectionUiState(
                preferences.source,
                preferences.sourceApps,
                saving || preferences.saving,
            ),
            sourceError = preferences.error == SettingsError.Source,
            folderConflict = preferences.error == SettingsError.FolderConflict,
        ),
        actions = OnboardingActions(
            mode = settings::setDisplayMode,
            next = onboarding::next,
            back = onboarding::back,
            finish = onboarding::finish,
            requestLauncher = launcher.request,
            connection = SourceConnectionActions(
                settings::selectSourceApp,
                settings::requestFolder,
                settings::replaceBookFolder,
                settings::removeBookFolder,
            ),
        ),
        contentPadding = padding,
    )
}
