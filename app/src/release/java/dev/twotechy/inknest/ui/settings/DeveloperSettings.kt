package dev.twotechy.inknest.ui.settings

import androidx.compose.runtime.Composable
import dev.twotechy.inknest.data.preview.SourcePreviewController
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.ui.home.HomeViewModel

@Composable
internal fun DeveloperSettingsRoute(
    homeViewModel: HomeViewModel,
    sources: SourcePreviewController?,
    container: AppContainer,
) = Unit
