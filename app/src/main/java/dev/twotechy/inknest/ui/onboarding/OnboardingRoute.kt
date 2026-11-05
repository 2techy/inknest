package dev.twotechy.inknest.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.ui.components.LauncherActions
import dev.twotechy.inknest.ui.settings.SettingsError
import dev.twotechy.inknest.ui.settings.SettingsViewModel
import dev.twotechy.inknest.ui.settings.SourceConnectionUiState
import dev.twotechy.inknest.ui.settings.SourceFolderPicker

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
    val folder by settings.pendingFolder.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    DisposableEffect(settings, step, activity) {
        onDispose {
            if (activity?.isChangingConfigurations != true) settings.dismissFolder()
        }
    }
    BackHandler { if (!saving && !preferences.saving) onboarding.back() }
    SourceFolderPicker(
        folder = if (step == 1) folder else null,
        onSelect = { selected, uri, flags ->
            settings.dismissFolder()
            settings.connectTree(selected, uri, flags)
        },
        onDismiss = settings::dismissFolder,
    )
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
        ),
        actions = OnboardingActions(
            mode = settings::setDisplayMode,
            next = onboarding::next,
            back = onboarding::back,
            finish = onboarding::finish,
            requestLauncher = launcher.request,
            selectSourceApp = settings::selectSourceApp,
            selectFolder = settings::requestFolder,
        ),
        contentPadding = padding,
    )
}
