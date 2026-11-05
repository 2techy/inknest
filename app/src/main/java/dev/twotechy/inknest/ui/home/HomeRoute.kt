package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.ReadingItemOpener
import dev.twotechy.inknest.ui.components.rememberAppLaunchAction
import dev.twotechy.inknest.ui.components.rememberDirectReadingMode
import dev.twotechy.inknest.ui.components.rememberReadingOpenAction

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel,
    readingItemOpener: ReadingItemOpener?,
    sourceConfig: ReadingSourceConfigRepository?,
    appLauncher: AppLauncher,
    appIconLoader: AppIconLoader,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onNavigateToApps: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val direct = rememberDirectReadingMode(readingItemOpener, sourceConfig)
    val loaded = state as? HomeUiState.Loaded
    val content = loaded?.reading as? ReadingSectionState.Content
    val displayState = if (loaded != null && content != null) {
        loaded.copy(reading = content.copy(current = content.current.copy(canContinue = direct)))
    } else {
        state
    }
    val readingAction = rememberReadingOpenAction(readingItemOpener, onSettings)

    HomeScreen(
        state = displayState,
        actions = HomeActions(
            reading = {
                content?.current?.let { readingAction(it.id, it.availability) }
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
