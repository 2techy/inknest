package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.model.ReadingOpenState
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.ReadingItemDispatcher
import dev.twotechy.inknest.ui.components.ReadingOpenEffect
import dev.twotechy.inknest.ui.components.ReadingOpenErrorActions
import dev.twotechy.inknest.ui.components.rememberAppLaunchAction

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel,
    readingItemDispatcher: ReadingItemDispatcher,
    appLauncher: AppLauncher,
    appIconLoader: AppIconLoader,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onNavigateToApps: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val loaded = state as? HomeUiState.Loaded
    val content = loaded?.reading as? ReadingSectionState.Content
    ReadingOpenEffect(
        loaded?.opening ?: ReadingOpenState.Idle,
        viewModel.bookOpening.actions,
        readingItemDispatcher,
        ReadingOpenErrorActions(onSettings, viewModel::refresh),
    )

    HomeScreen(
        state = state,
        actions = HomeActions(
            reading = {
                content?.current?.let { viewModel.bookOpening.actions.open(it.id) }
            },
            refresh = viewModel::refresh,
            launchApp = rememberAppLaunchAction(appLauncher, viewModel::refreshApps),
            openApps = onNavigateToApps,
            settings = onSettings,
        ),
        modifier = modifier.padding(contentPadding),
        iconLoader = appIconLoader,
    )
}
