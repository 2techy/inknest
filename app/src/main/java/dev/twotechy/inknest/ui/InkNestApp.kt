package dev.twotechy.inknest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.ui.apps.AppsRoute
import dev.twotechy.inknest.ui.apps.AppsViewModel
import dev.twotechy.inknest.ui.components.rememberDefaultLauncher
import dev.twotechy.inknest.ui.home.HomeRoute
import dev.twotechy.inknest.ui.home.HomeViewModel
import dev.twotechy.inknest.ui.navigation.AppNavigationViewModel
import dev.twotechy.inknest.ui.navigation.InkNestBottomBar
import dev.twotechy.inknest.ui.navigation.InkNestNavigationRail
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.placeholder.PlaceholderScreen
import dev.twotechy.inknest.ui.settings.SettingsRoute

@Composable
internal fun InkNestApp(container: AppContainer, navigation: AppNavigationViewModel) {
    val homeViewModel: HomeViewModel = viewModel(factory = container.homeViewModelFactory)
    val appsViewModel: AppsViewModel = viewModel(factory = container.appsViewModelFactory)

    val selected by navigation.selected.collectAsStateWithLifecycle()
    val tab = TopLevelDestination.valueOf(selected)
    val saveableStateHolder = rememberSaveableStateHolder()

    val isDefaultLauncher by rememberDefaultLauncher(container.launcherModeController)

    // Back behavior according to plan M9:
    // - From other tabs -> navigate back to Home
    // - At Home:
    //     - If default launcher: do nothing (stay at Home)
    //     - If standalone app: BackHandler disabled -> system exits Activity
    BackHandler(enabled = tab != TopLevelDestination.Home || isDefaultLauncher) {
        if (tab != TopLevelDestination.Home) {
            navigation.goHome()
        }
        // If at Home and isDefaultLauncher is true: consume back gesture to stay on Home launcher
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { if (!useRail) InkNestBottomBar(tab) { navigation.select(it) } },
        ) { insets ->
            Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                if (useRail) InkNestNavigationRail(tab) { navigation.select(it) }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    BoxWithConstraints(Modifier.widthIn(max = 800.dp).fillMaxSize()) {
                        val gutter = if (maxWidth < 360.dp) {
                            12.dp
                        } else if (maxWidth < 600.dp) {
                            16.dp
                        } else {
                            32.dp
                        }
                        val padding = PaddingValues(horizontal = gutter, vertical = 20.dp)
                        saveableStateHolder.SaveableStateProvider(tab.name) {
                            when (tab) {
                                TopLevelDestination.Home -> HomeRoute(
                                    viewModel = homeViewModel,
                                    appLauncher = container.appLauncher,
                                    appIconLoader = container.appIconLoader,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = padding,
                                    onNavigateToApps = {
                                        navigation.select(TopLevelDestination.Apps)
                                    },
                                    onSettings = {
                                        navigation.select(TopLevelDestination.Settings)
                                    },
                                )

                                TopLevelDestination.Apps -> AppsRoute(
                                    viewModel = appsViewModel,
                                    appLauncher = container.appLauncher,
                                    appIconLoader = container.appIconLoader,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = padding,
                                )

                                TopLevelDestination.Settings -> SettingsRoute(
                                    homeViewModel = homeViewModel,
                                    launcherModeController = container.launcherModeController,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = padding,
                                )

                                else -> PlaceholderScreen(tab, Modifier.fillMaxSize(), padding)
                            }
                        }
                    }
                }
            }
        }
    }
}
