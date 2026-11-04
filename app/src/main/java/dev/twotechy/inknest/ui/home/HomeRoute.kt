package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.ReadingItemOpener
import dev.twotechy.inknest.ui.components.rememberAppLaunchAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

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

    val config by (sourceConfig?.config ?: flowOf(ReadingSourceConfig()))
        .collectAsStateWithLifecycle(ReadingSourceConfig())
    var resume by remember { mutableIntStateOf(0) }
    var direct by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resume++ }
    LaunchedEffect(config, resume) {
        direct = withContext(Dispatchers.IO) { readingItemOpener?.isDirect(config) == true }
    }
    val loaded = state as? HomeUiState.Loaded
    val content = loaded?.reading as? ReadingSectionState.Content
    val displayState = if (loaded != null && content != null) {
        loaded.copy(reading = content.copy(current = content.current.copy(canContinue = direct)))
    } else {
        state
    }
    val readingAction = rememberReadingOpenAction(state, readingItemOpener, onSettings)

    HomeScreen(
        state = displayState,
        actions = HomeActions(
            reading = readingAction,
            refresh = viewModel::refresh,
            launchApp = rememberAppLaunchAction(appLauncher, viewModel::refreshApps),
            openApps = onNavigateToApps,
            settings = onSettings,
        ),
        modifier = modifier.padding(contentPadding),
        iconLoader = appIconLoader,
    )
}
