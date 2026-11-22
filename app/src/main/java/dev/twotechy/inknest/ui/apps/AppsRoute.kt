package dev.twotechy.inknest.ui.apps

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.apps.AppLauncher
import dev.twotechy.inknest.platform.icons.AppIconLoader
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
        modifier = modifier,
        contentPadding = contentPadding,
        iconLoader = appIconLoader,
        actions = AppsActions(
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSearchActiveChanged = viewModel::onSearchActiveChanged,
            onToggleViewMode = viewModel::toggleViewMode,
            item = AppItemActions(
                launch = rememberAppLaunchAction(appLauncher, viewModel::refresh),
                openInfo = { componentId ->
                    if (!appLauncher.openAppInfo(componentId)) {
                        Toast.makeText(
                            context,
                            R.string.app_info_unavailable,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                togglePin = { componentId, isPinned ->
                    if (isPinned) {
                        viewModel.unpin(componentId)
                    } else {
                        viewModel.pin(componentId)
                    }
                },
                movePin = viewModel::movePin,
            ),
            onRetry = viewModel::refresh,
            onDismissError = viewModel::dismissActionError,
        ),
    )
}
