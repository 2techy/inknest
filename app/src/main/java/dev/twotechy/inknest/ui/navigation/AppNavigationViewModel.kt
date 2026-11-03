package dev.twotechy.inknest.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

internal class AppNavigationViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val selected = savedState.getStateFlow("selectedDestination", TopLevelDestination.Home.name)
    private var initialLaunchHandled = false

    fun handleInitialLaunch(isHome: Boolean, fromHistory: Boolean) {
        if (initialLaunchHandled) return
        initialLaunchHandled = true
        if (isHome && !fromHistory) goHome()
    }

    fun select(destination: TopLevelDestination) {
        savedState["selectedDestination"] = destination.name
    }

    // HOME updates durable state even before Compose subscribes; no transient event can be lost.
    fun goHome() = select(TopLevelDestination.Home)

    companion object {
        val Factory = viewModelFactory {
            initializer { AppNavigationViewModel(createSavedStateHandle()) }
        }
    }
}
