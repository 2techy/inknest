package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.clock.rememberMinuteClock
import dev.twotechy.inknest.ui.components.rememberAppLaunchAction

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel,
    appLauncher: AppLauncher,
    appIconLoader: AppIconLoader,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onNavigateToApps: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tick = rememberMinuteClock()

    HomeScreen(
        clockTick = tick,
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        iconLoader = appIconLoader,
        onReadingAction = onSettings,
        onRefresh = viewModel::refresh,
        onAppClick = rememberAppLaunchAction(appLauncher, viewModel::refreshApps),
        onNavigateToApps = onNavigateToApps,
        onSettings = onSettings,
    )
}
