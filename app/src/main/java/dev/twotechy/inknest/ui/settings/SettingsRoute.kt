package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.ui.home.HomeViewModel

@Composable
internal fun SettingsRoute(
    homeViewModel: HomeViewModel,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    // The preview controller is absent in release. Real settings get their own VM in M12.
    val previewMode by homeViewModel.previewMode.collectAsStateWithLifecycle()
    SettingsScreen(previewMode, modifier, contentPadding, homeViewModel::onPreviewModeChanged)
}
