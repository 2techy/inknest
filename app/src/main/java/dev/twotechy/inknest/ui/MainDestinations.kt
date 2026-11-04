package dev.twotechy.inknest.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.ui.apps.AppsRoute
import dev.twotechy.inknest.ui.apps.AppsViewModel
import dev.twotechy.inknest.ui.home.HomeRoute
import dev.twotechy.inknest.ui.home.HomeViewModel
import dev.twotechy.inknest.ui.navigation.AppNavigationViewModel
import dev.twotechy.inknest.ui.navigation.TopLevelDestination

@Composable
internal fun HomeDestination(
    container: AppContainer,
    navigation: AppNavigationViewModel,
    padding: PaddingValues,
) {
    val home: HomeViewModel = viewModel(factory = container.homeViewModelFactory)
    HomeRoute(
        viewModel = home,
        readingItemOpener = container.readingItemOpener,
        sourceConfig = container.readingSourceConfigRepository,
        appLauncher = container.appLauncher,
        appIconLoader = container.appIconLoader,
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        onNavigateToApps = { navigation.select(TopLevelDestination.Apps) },
        onSettings = { navigation.select(TopLevelDestination.Settings) },
    )
}

@Composable
internal fun AppsDestination(container: AppContainer, padding: PaddingValues) {
    val apps: AppsViewModel = viewModel(factory = container.appsViewModelFactory)
    AppsRoute(
        viewModel = apps,
        appLauncher = container.appLauncher,
        appIconLoader = container.appIconLoader,
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
    )
}
