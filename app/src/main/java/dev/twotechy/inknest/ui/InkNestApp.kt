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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twotechy.inknest.ui.home.HomeRoute
import dev.twotechy.inknest.ui.home.HomeViewModel
import dev.twotechy.inknest.ui.navigation.InkNestBottomBar
import dev.twotechy.inknest.ui.navigation.InkNestNavigationRail
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.placeholder.PlaceholderScreen
import dev.twotechy.inknest.ui.settings.SettingsRoute

@Composable
internal fun InkNestApp(homeViewModelFactory: ViewModelProvider.Factory) {
    // Activity scope shares Home state with debug controls and survives tab changes.
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
    var selected by rememberSaveable { mutableStateOf(TopLevelDestination.Home.name) }
    val tab = TopLevelDestination.valueOf(selected)
    BackHandler(enabled = tab != TopLevelDestination.Home) {
        selected =
            TopLevelDestination.Home.name
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Use the app window, not a device model, resolution or physical diagonal.
        val useRail = maxWidth >= 600.dp
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { if (!useRail) InkNestBottomBar(tab) { selected = it.name } },
        ) { insets ->
            Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                if (useRail) InkNestNavigationRail(tab) { selected = it.name }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    BoxWithConstraints(Modifier.widthIn(max = 800.dp).fillMaxSize()) {
                        val gutter = if (maxWidth <
                            360.dp
                        ) {
                            12.dp
                        } else if (maxWidth < 600.dp) {
                            16.dp
                        } else {
                            32.dp
                        }
                        val padding = PaddingValues(horizontal = gutter, vertical = 20.dp)
                        when (tab) {
                            TopLevelDestination.Home -> HomeRoute(
                                homeViewModel,
                                Modifier.fillMaxSize(),
                                padding,
                            ) {
                                selected = TopLevelDestination.Settings.name
                            }

                            TopLevelDestination.Settings -> SettingsRoute(
                                homeViewModel,
                                Modifier.fillMaxSize(),
                                padding,
                            )

                            else -> PlaceholderScreen(tab, Modifier.fillMaxSize(), padding)
                        }
                    }
                }
            }
        }
    }
}
