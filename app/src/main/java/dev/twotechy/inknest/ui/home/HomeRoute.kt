package dev.twotechy.inknest.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val previewMode by viewModel.previewMode.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tick = rememberMinuteClock()
    val context = LocalContext.current

    HomeScreen(
        clockTick = tick,
        state = state,
        sampleData =
            previewMode != null &&
                previewMode != dev.twotechy.inknest.data.preview.HomePreviewMode.Off,
        modifier = modifier,
        contentPadding = contentPadding,
        iconLoader = appIconLoader,
        onReadingAction = {
            Toast.makeText(
                context,
                "Sample data only. Reading integration is coming later.",
                Toast.LENGTH_SHORT,
            ).show()
        },
        onAppClick = rememberAppLaunchAction(appLauncher, viewModel::refreshApps),
        onNavigateToApps = onNavigateToApps,
        onSettings = onSettings,
    )
}
