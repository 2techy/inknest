package dev.twotechy.inknest.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.ui.settings.SourceFolderSelection
import dev.twotechy.inknest.ui.shared.SourceFolderActions
import kotlinx.coroutines.launch

@Composable
internal fun OnboardingRoute(
    onboarding: OnboardingViewModel,
    isDefault: Boolean,
    requestLauncher: () -> Unit,
    padding: PaddingValues,
) {
    val state by onboarding.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
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
            requestLauncher = {
                scope.launch {
                    if (onboarding.prepareLauncherRequest()) requestLauncher()
                }
            },
            folders = SourceFolderActions(
                onboarding::requestFolder,
                onboarding::replaceBookFolder,
                onboarding::removeBookFolder,
            ),
        ),
        contentPadding = padding,
    )
}
