package dev.twotechy.inknest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.ui.apps.AppsRoute
import dev.twotechy.inknest.ui.apps.AppsViewModel
import dev.twotechy.inknest.ui.components.AppScaffold
import dev.twotechy.inknest.ui.components.LauncherActions
import dev.twotechy.inknest.ui.components.rememberDefaultLauncher
import dev.twotechy.inknest.ui.components.rememberLauncherActions
import dev.twotechy.inknest.ui.home.HomeRoute
import dev.twotechy.inknest.ui.home.HomeViewModel
import dev.twotechy.inknest.ui.navigation.AppNavigationViewModel
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.onboarding.OnboardingRoute
import dev.twotechy.inknest.ui.onboarding.OnboardingViewModel
import dev.twotechy.inknest.ui.placeholder.PlaceholderScreen
import dev.twotechy.inknest.ui.settings.DeveloperSettingsRoute
import dev.twotechy.inknest.ui.settings.SettingsError
import dev.twotechy.inknest.ui.settings.SettingsPage
import dev.twotechy.inknest.ui.settings.SettingsRoute
import dev.twotechy.inknest.ui.settings.SettingsUiState
import dev.twotechy.inknest.ui.settings.SettingsViewModel
import dev.twotechy.inknest.ui.theme.InkNestTheme

@Composable
internal fun InkNestApp(container: AppContainer, navigation: AppNavigationViewModel) {
    val settings: SettingsViewModel = viewModel(factory = container.settingsViewModelFactory)
    val onboarding: OnboardingViewModel = viewModel(factory = container.onboardingViewModelFactory)
    val state by settings.uiState.collectAsStateWithLifecycle()
    val completion by onboarding.completion.collectAsStateWithLifecycle()
    LaunchedEffect(state.preferences?.onboardingCompleted) {
        state.preferences?.let { navigation.offerSetup(it.onboardingCompleted) }
    }
    LaunchedEffect(completion) {
        if (completion > 0) {
            navigation.goHome()
            onboarding.acknowledgeCompletion()
        }
    }
    InkNestTheme(state.preferences ?: AppPreferences()) {
        InkNestContent(container, navigation, settings, onboarding, state)
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
    val homeViewModel: HomeViewModel = viewModel(factory = container.homeViewModelFactory)
    val appsViewModel: AppsViewModel = viewModel(factory = container.appsViewModelFactory)
    val selected by navigation.selected.collectAsStateWithLifecycle()
    val tab = TopLevelDestination.valueOf(selected)
    val pageName by navigation.settingsPage.collectAsStateWithLifecycle()
    val page = SettingsPage.entries.find { it.name == pageName } ?: SettingsPage.Overview
    val setupVisible by navigation.onboardingVisible.collectAsStateWithLifecycle()
    val saveableStateHolder = rememberSaveableStateHolder()
    val isDefaultLauncher by rememberDefaultLauncher(container.launcherModeController)
    val launcher = rememberLauncherActions(container.launcherModeController)
    val openPage = { value: SettingsPage -> navigation.openSettingsPage(value.name) }
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

    val showNavigation = !setupVisible &&
        (tab != TopLevelDestination.Settings || page == SettingsPage.Overview)
    AppScaffold(tab, navigation::select, showNavigation) { padding ->
        if (setupVisible && settingsState.preferences != null) {
            OnboardingRoute(
                onboarding = onboarding,
                displayMode = settingsState.preferences.displayMode,
                preferencesSaving = settingsState.saving,
                preferencesSaveError = settingsState.error == SettingsError.Save,
                onDisplayMode = settings::setDisplayMode,
                isDefault = isDefaultLauncher,
                launcher = launcher,
                padding = padding,
            )
        } else {
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
                        viewModel = settings,
                        page = page,
                        openPage = openPage,
                        launcher = launcher,
                        isDefaultLauncher = isDefaultLauncher,
                        developerTools = {
                            DeveloperSettingsRoute(homeViewModel, container.sourcePreviewController)
                        },
                        onReviewSetup = {
                            onboarding.restart()
                            navigation.reviewSetup()
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                    )

                    else -> PlaceholderScreen(tab, Modifier.fillMaxSize(), padding)
                }
            }
        }
    }
}
