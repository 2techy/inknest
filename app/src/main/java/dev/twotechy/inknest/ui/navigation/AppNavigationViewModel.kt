package dev.twotechy.inknest.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class SetupEntry { FirstRun, Review }

internal class AppNavigationViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val selected = savedState.getStateFlow("selectedDestination", TopLevelDestination.Home.name)
    val settingsPage = savedState.getStateFlow("settingsPage", "Overview")
    val onboardingVisible = savedState.getStateFlow("onboardingVisible", false)
    private val session = MutableStateFlow(0L)
    val gestureSession = session.asStateFlow()
    private var setupOffered = false

    fun openSettingsPage(page: String) {
        savedState["settingsPage"] = page
    }
    fun offerSetup(completed: Boolean) {
        val entry = savedState.get<String>("setupEntry") ?: SetupEntry.FirstRun.name
        if (completed && entry == SetupEntry.FirstRun.name) {
            if (onboardingVisible.value) goHome()
        }
        if (setupOffered) return
        setupOffered = true
        if (!completed) {
            savedState["setupEntry"] = SetupEntry.FirstRun.name
            savedState["onboardingVisible"] = true
        }
    }
    fun finishSetup(finished: Boolean) {
        if (finished && onboardingVisible.value) goHome()
    }
    fun reviewSetup() {
        savedState["setupEntry"] = SetupEntry.Review.name
        setupOffered = true
        savedState["settingsPage"] = "Overview"
        savedState["onboardingVisible"] = true
    }
    private var initialLaunchHandled = false

    fun handleInitialLaunch(isHome: Boolean, fromHistory: Boolean, restoring: Boolean = false) {
        if (initialLaunchHandled) return
        initialLaunchHandled = true
        if (restoring) return
        if (isHome && !fromHistory) {
            savedState["selectedDestination"] = TopLevelDestination.Home.name
            savedState["settingsPage"] = "Overview"
            savedState["onboardingVisible"] = false
        }
    }

    fun select(destination: TopLevelDestination) {
        session.value++
        savedState["settingsPage"] = "Overview"
        savedState["onboardingVisible"] = false
        setupOffered = true
        savedState["selectedDestination"] = destination.name
    }

    // HOME updates durable state even before Compose subscribes; no transient event can be lost.
    fun goHome() {
        select(TopLevelDestination.Home)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AppNavigationViewModel(createSavedStateHandle()) }
        }
    }
}
