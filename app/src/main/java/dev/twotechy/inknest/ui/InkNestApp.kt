package dev.twotechy.inknest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twotechy.inknest.di.AppContainer
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.ui.components.AppScaffold
import dev.twotechy.inknest.ui.components.LocalBookCoverLoader
import dev.twotechy.inknest.ui.components.LocalContentGestureSession
import dev.twotechy.inknest.ui.components.rememberDefaultLauncher
import dev.twotechy.inknest.ui.components.rememberLauncherActions
import dev.twotechy.inknest.ui.navigation.AppNavigationViewModel
import dev.twotechy.inknest.ui.navigation.AppsDestination
import dev.twotechy.inknest.ui.navigation.DetailDestinations
import dev.twotechy.inknest.ui.navigation.HomeDestination
import dev.twotechy.inknest.ui.navigation.LibraryDestination
import dev.twotechy.inknest.ui.navigation.StatsDestination
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.onboarding.OnboardingRoute
import dev.twotechy.inknest.ui.onboarding.OnboardingViewModel
import dev.twotechy.inknest.ui.settings.SettingsEnvironment
import dev.twotechy.inknest.ui.settings.SettingsNavigation
import dev.twotechy.inknest.ui.settings.SettingsPage
import dev.twotechy.inknest.ui.settings.SettingsRoute
import dev.twotechy.inknest.ui.settings.SettingsViewModel
import dev.twotechy.inknest.ui.theme.InkNestTheme
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter

@Composable
internal fun InkNestApp(container: AppContainer, navigation: AppNavigationViewModel) {
    val app: AppViewModel = viewModel(factory = container.appViewModelFactory)
    LifecycleEventEffect(Lifecycle.Event.ON_START) { app.onForeground() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(app, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { app.runUpdateChecks() }
    }
    val locales = LocalConfiguration.current.locales.toLanguageTags()
    LaunchedEffect(locales) { app.onLocalesChanged(locales) }
    val settings: SettingsViewModel = viewModel(factory = container.settingsViewModelFactory)
    val onboarding: OnboardingViewModel = viewModel(factory = container.onboardingViewModelFactory)
    val preferences by app.preferences.collectAsStateWithLifecycle()
    LaunchedEffect(preferences?.onboardingCompleted) {
        preferences?.let { prefs ->
            if (prefs.onboardingLauncherPending) {
                navigation.restoreLauncherSetup(
                    prefs.onboardingCompleted,
                    onboarding::restoreLauncherStep,
                )
            } else {
                navigation.offerSetup(prefs.onboardingCompleted)
            }
        }
    }
    OnboardingNavigationEffects(onboarding, navigation)
    CompositionLocalProvider(
        LocalContentGestureSession provides navigation.gestureSession,
        LocalBookCoverLoader provides container.bookCoverLoader,
    ) {
        InkNestTheme(preferences ?: AppPreferences()) {
            InkNestContent(container, navigation, settings, onboarding)
        }
    }
}

@Composable
private fun OnboardingNavigationEffects(
    onboarding: OnboardingViewModel,
    navigation: AppNavigationViewModel,
) {
    val state by onboarding.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { navigation.finishSetup(state.finished) }
    val leavingLauncherStep = state.preferences != null && state.step != 2
    LaunchedEffect(leavingLauncherStep) {
        if (leavingLauncherStep) navigation.cancelOnboardingLauncherRequest()
    }
    LaunchedEffect(onboarding, navigation) {
        navigation.onboardingVisible.dropWhile { !it }.filter { !it }.collect {
            onboarding.discardLauncherCheckpoint()
        }
    }
}

@Composable
private fun InkNestContent(
    container: AppContainer,
    navigation: AppNavigationViewModel,
    settings: SettingsViewModel,
    onboarding: OnboardingViewModel,
) {
    val selected by navigation.selected.collectAsStateWithLifecycle()
    val tab = TopLevelDestination.valueOf(selected)
    val pageName by navigation.settingsPage.collectAsStateWithLifecycle()
    val page = SettingsPage.entries.find { it.name == pageName } ?: SettingsPage.Overview
    val setupVisible by navigation.onboardingVisible.collectAsStateWithLifecycle()
    val details by navigation.detailStack.collectAsStateWithLifecycle()
    val saveableStateHolder = rememberSaveableStateHolder()
    val isDefaultLauncher by rememberDefaultLauncher(container.launcherModeController)
    val launcher = rememberLauncherActions(container.launcherModeController) {
        navigation.cancelOnboardingLauncherRequest()
        if (setupVisible) onboarding.discardLauncherCheckpoint()
    }
    val openPage = { value: SettingsPage -> navigation.openSettingsPage(value.name) }
    HomeBackHandler(tab, isDefaultLauncher, navigation::goHome)

    val showNavigation = !setupVisible && tab != TopLevelDestination.Settings &&
        details.isEmpty()
    AppScaffold(tab, navigation::select, showNavigation) { padding ->
        if (setupVisible) {
            OnboardingRoute(
                onboarding = onboarding,
                isDefault = isDefaultLauncher,
                requestLauncher = {
                    navigation.beginOnboardingLauncherRequest()
                    launcher.request()
                },
                padding = padding,
            )
        } else if (details.isNotEmpty()) {
            DetailDestinations(details, container, navigation, padding)
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
                        navigation = SettingsNavigation(page, openPage, navigation::leaveSettings),
                        environment = SettingsEnvironment(
                            launcher,
                            container.appLanguageController,
                            isDefaultLauncher,
                            container.appUpdateInstaller,
                        ),
                        onReviewSetup = {
                            onboarding.restart()
                            navigation.reviewSetup()
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = padding,
                    )

                    TopLevelDestination.Stats ->
                        StatsDestination(container, navigation, padding)
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
