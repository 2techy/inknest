package dev.twotechy.inknest.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.ui.components.LauncherActions
import dev.twotechy.inknest.ui.settings.SourceConnectionActions
import dev.twotechy.inknest.ui.settings.SourceFolderSelection

@Composable
internal fun OnboardingRoute(
    onboarding: OnboardingViewModel,
    isDefault: Boolean,
    launcher: LauncherActions,
    padding: PaddingValues,
) {
    val state by onboarding.uiState.collectAsStateWithLifecycle()
    SourceFolderSelection(
        state.pendingFolder.takeIf { state.step == 1 },
        onboarding::acceptFolder,
        onboarding::dismissFolder,
        state.step,
    )
    BackHandler { if (!state.saving) onboarding.back() }
    if (state.preferences == null) return
    OnboardingScreen(
        state = state.copy(isDefaultLauncher = isDefault),
        actions = OnboardingActions(
            mode = onboarding::setDisplayMode,
            next = onboarding::next,
            back = onboarding::back,
            finish = onboarding::finish,
            requestLauncher = launcher.request,
            connection = SourceConnectionActions(
                onboarding::selectSourceApp,
                onboarding::requestFolder,
                onboarding::replaceBookFolder,
                onboarding::removeBookFolder,
            ),
        ),
        contentPadding = padding,
    )
}
