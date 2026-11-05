package dev.twotechy.inknest.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.components.LauncherActions

@Composable
internal fun OnboardingRoute(
    onboarding: OnboardingViewModel,
    displayMode: DisplayMode,
    preferencesSaving: Boolean,
    preferencesSaveError: Boolean,
    onDisplayMode: (DisplayMode) -> Unit,
    isDefault: Boolean,
    launcher: LauncherActions,
    padding: PaddingValues,
) {
    val step by onboarding.step.collectAsStateWithLifecycle()
    val saving by onboarding.saving.collectAsStateWithLifecycle()
    val error by onboarding.error.collectAsStateWithLifecycle()
    BackHandler { if (!saving && !preferencesSaving) onboarding.back() }
    OnboardingScreen(
        state = OnboardingUiState(
            step = step,
            mode = displayMode,
            saving = saving || preferencesSaving,
            saveError = error,
            preferenceError = preferencesSaveError,
            isDefaultLauncher = isDefault,
        ),
        actions = OnboardingActions(
            mode = onDisplayMode,
            next = onboarding::next,
            back = onboarding::back,
            finish = onboarding::finish,
            requestLauncher = launcher.request,
        ),
        contentPadding = padding,
    )
}
