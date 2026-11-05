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
import dev.twotechy.inknest.ui.library.LibraryRoute
import dev.twotechy.inknest.ui.library.LibraryViewModel
import dev.twotechy.inknest.ui.navigation.AppNavigationViewModel
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.settings.SettingsPage
import dev.twotechy.inknest.ui.stats.StatsRoute
import dev.twotechy.inknest.ui.stats.StatsViewModel

@Composable
internal fun StatsDestination(
    container: AppContainer,
    navigation: AppNavigationViewModel,
    padding: PaddingValues,
) {
    val stats: StatsViewModel = viewModel(factory = container.statsViewModelFactory)
    StatsRoute(stats, {
        navigation.select(TopLevelDestination.Settings)
        navigation.openSettingsPage(SettingsPage.ReadingData.name)
    }, Modifier.fillMaxSize(), padding)
}

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

@Composable
internal fun LibraryDestination(
    container: AppContainer,
    navigation: AppNavigationViewModel,
    padding: PaddingValues,
) {
    val library: LibraryViewModel = viewModel(factory = container.libraryViewModelFactory)
    LibraryRoute(
        viewModel = library,
        opener = container.readingItemOpener,
        source = container.readingSourceConfigRepository,
        onSettings = {
            navigation.select(TopLevelDestination.Settings)
            navigation.openSettingsPage(SettingsPage.ReadingData.name)
        },
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
    )
}
