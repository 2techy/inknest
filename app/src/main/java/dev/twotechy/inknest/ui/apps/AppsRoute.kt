package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LaunchResult
import dev.twotechy.inknest.ui.components.rememberAppLaunchAction

@Composable
internal fun AppsRoute(
    viewModel: AppsViewModel,
    appLauncher: AppLauncher,
    appIconLoader: AppIconLoader,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppsScreen(
        state = state,
        modifier = modifier.padding(contentPadding),
        iconLoader = appIconLoader,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSearchActiveChanged = viewModel::onSearchActiveChanged,
        onToggleViewMode = viewModel::toggleViewMode,
        onAppClick = rememberAppLaunchAction(appLauncher, viewModel::refresh),
        onTogglePin = { componentId, isPinned ->
            if (isPinned) {
                viewModel.unpin(componentId)
            } else {
                viewModel.pin(componentId)
            }
        },
        onMovePin = viewModel::movePin,
        onRetry = viewModel::refresh,
        onDismissError = viewModel::dismissActionError,
    )
}
