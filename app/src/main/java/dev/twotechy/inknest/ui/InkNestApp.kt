package dev.twotechy.inknest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.ui.components.AppScaffold
import dev.twotechy.inknest.ui.components.LocalBookCoverLoader
import dev.twotechy.inknest.ui.components.LocalContentGestureSession
import dev.twotechy.inknest.ui.components.rememberDefaultLauncher
import dev.twotechy.inknest.ui.components.rememberLauncherActions
import dev.twotechy.inknest.ui.navigation.AppNavigationViewModel
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.onboarding.OnboardingRoute
import dev.twotechy.inknest.ui.onboarding.OnboardingViewModel
import dev.twotechy.inknest.ui.settings.SettingsPage
import dev.twotechy.inknest.ui.settings.SettingsRoute
import dev.twotechy.inknest.ui.settings.SettingsUiState
import dev.twotechy.inknest.ui.settings.SettingsViewModel
import dev.twotechy.inknest.ui.theme.InkNestTheme

@Composable
internal fun InkNestApp(container: AppContainer, navigation: AppNavigationViewModel) {
    val app: AppViewModel = viewModel(factory = container.appViewModelFactory)
    LifecycleEventEffect(Lifecycle.Event.ON_START) { app.onForeground() }
    val settings: SettingsViewModel = viewModel(factory = container.settingsViewModelFactory)
    val onboarding: OnboardingViewModel = viewModel(factory = container.onboardingViewModelFactory)
    val state by settings.uiState.collectAsStateWithLifecycle()
    val setup by onboarding.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.preferences?.onboardingCompleted) {
        state.preferences?.let { navigation.offerSetup(it.onboardingCompleted) }
    }
    LaunchedEffect(setup.finished) {
        navigation.finishSetup(setup.finished)
    }
    CompositionLocalProvider(
        LocalContentGestureSession provides navigation.gestureSession,
        LocalBookCoverLoader provides container.bookCoverLoader,
    ) {
        InkNestTheme(state.preferences ?: AppPreferences()) {
            InkNestContent(container, navigation, settings, onboarding, state)
        }
    }
}

@Composable
private fun InkNestContent(
    container: AppContainer,
    navigation: AppNavigationViewModel,
    settings: SettingsViewModel,
    onboarding: OnboardingViewModel,
    settingsState: SettingsUiState,
) {
    val selected by navigation.selected.collectAsStateWithLifecycle()
    val tab = TopLevelDestination.valueOf(selected)
    val pageName by navigation.settingsPage.collectAsStateWithLifecycle()
    val page = SettingsPage.entries.find { it.name == pageName } ?: SettingsPage.Overview
    val setupVisible by navigation.onboardingVisible.collectAsStateWithLifecycle()
    val saveableStateHolder = rememberSaveableStateHolder()
    val isDefaultLauncher by rememberDefaultLauncher(container.launcherModeController)
    val launcher = rememberLauncherActions(container.launcherModeController)
    val openPage = { value: SettingsPage -> navigation.openSettingsPage(value.name) }
    HomeBackHandler(tab, isDefaultLauncher, navigation::goHome)

    val showNavigation = !setupVisible &&
        (tab != TopLevelDestination.Settings || page == SettingsPage.Overview)
    AppScaffold(tab, navigation::select, showNavigation) { padding ->
        if (setupVisible && settingsState.preferences != null) {
            OnboardingRoute(
                onboarding = onboarding,
                isDefault = isDefaultLauncher,
                launcher = launcher,
                padding = padding,
            )
        } else {
            saveableStateHolder.SaveableStateProvider(tab.name) {
                when (tab) {
                    TopLevelDestination.Library -> LibraryDestination(
                        container,
                        navigation,
                        padding,
                    )

                    TopLevelDestination.Home -> HomeDestination(container, navigation, padding)

                    TopLevelDestination.Apps -> AppsDestination(container, padding)

                    TopLevelDestination.Settings -> SettingsRoute(
                        viewModel = settings,
                        page = page,
                        openPage = openPage,
                        launcher = launcher,
                        isDefaultLauncher = isDefaultLauncher,
                        onReviewSetup = {
                            onboarding.restart()
                            navigation.reviewSetup()
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                    )

                    TopLevelDestination.Stats -> StatsDestination(container, navigation, padding)
                }
            }
        }
    }
}

@Composable
private fun HomeBackHandler(
    tab: TopLevelDestination,
    isDefaultLauncher: Boolean,
    goHome: () -> Unit,
) {
    // Standalone Home lets the system handle Back; the default launcher stays at Home.
    BackHandler(enabled = tab != TopLevelDestination.Home || isDefaultLauncher) {
        if (tab != TopLevelDestination.Home) goHome()
    }
}
