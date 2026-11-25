package dev.twotechy.inknest.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.twotechy.inknest.model.ReadingItemId
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AppNavigationViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val selected = savedState.getStateFlow("selectedDestination", TopLevelDestination.Home.name)
    val settingsPage = savedState.getStateFlow("settingsPage", "Overview")
    private val details = MutableStateFlow(
        savedState.get<ArrayList<String>>("detailStack").orEmpty()
            .chunked(3).mapNotNull(DetailDestination::restore),
    )
    val detailStack = details.asStateFlow()
    val onboardingVisible = savedState.getStateFlow("onboardingVisible", false)
    private val session = MutableStateFlow(0L)
    val gestureSession = session.asStateFlow()
    private var setupOffered = false

    fun openSettingsPage(page: String) {
        savedState["settingsPage"] = page
    }
    fun openStatsDay(day: LocalDate) {
        if (onboardingVisible.value) return
        if (selected.value == TopLevelDestination.Stats.name && details.value.isEmpty()) {
            setDetails(listOf(DetailDestination.StatsDay(day)))
        }
    }

    fun openBook(id: ReadingItemId) {
        if (onboardingVisible.value) return
        val stack = details.value
        val canOpen = when (selected.value) {
            TopLevelDestination.Library.name -> stack.isEmpty()
            TopLevelDestination.Stats.name -> stack.singleOrNull() is DetailDestination.StatsDay
            else -> false
        }
        if (canOpen) setDetails(stack + DetailDestination.Book(id))
    }

    fun backFromDetail() {
        setDetails(details.value.dropLast(1))
    }

    private fun setDetails(stack: List<DetailDestination>) {
        savedState["detailStack"] = ArrayList(stack.flatMap(DetailDestination::savedParts))
        details.value = stack
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
    fun restoreLauncherSetup(completed: Boolean, restoreStep: () -> Unit) {
        if (setupOffered) return
        restoreStep()
        savedState["setupEntry"] =
            if (completed) SetupEntry.Review.name else SetupEntry.FirstRun.name
        setupOffered = true
        savedState["onboardingVisible"] = true
    }
    fun reviewSetup() {
        cancelOnboardingLauncherRequest()
        setDetails(emptyList())
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
            setDetails(emptyList())
            savedState["selectedDestination"] = TopLevelDestination.Home.name
            savedState["settingsPage"] = "Overview"
            savedState["onboardingVisible"] = false
        }
    }

    fun searchLibrary() {
        select(TopLevelDestination.Library)
        savedState["pendingLibrarySearch"] = true
    }

    fun consumeLibrarySearch(): Boolean {
        val pending = savedState.get<Boolean>("pendingLibrarySearch") == true
        savedState["pendingLibrarySearch"] = false
        return pending
    }

    fun leaveSettings() {
        val previous = savedState.get<String>("settingsReturnDestination")
        select(MainDestinations.find { it.name == previous } ?: TopLevelDestination.Home)
    }

    fun select(destination: TopLevelDestination) {
        if (destination == TopLevelDestination.Settings && selected.value != destination.name) {
            savedState["settingsReturnDestination"] = selected.value
        }
        savedState["pendingLibrarySearch"] = false
        setDetails(emptyList())
        cancelOnboardingLauncherRequest()
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

    fun beginOnboardingLauncherRequest() {
        if (onboardingVisible.value) savedState[ONBOARDING_LAUNCHER_REQUEST] = true
    }

    fun cancelOnboardingLauncherRequest() {
        savedState.remove<Boolean>(ONBOARDING_LAUNCHER_REQUEST)
    }

    fun handleHomeIntent(fromHomeKey: Boolean, isDefaultLauncher: () -> Boolean) {
        // Android can bring the newly selected HOME app forward before its role result arrives.
        val pending = savedState.remove<Boolean>(ONBOARDING_LAUNCHER_REQUEST) == true
        val setupHandoff = pending && onboardingVisible.value
        val automaticHandoff = setupHandoff && !fromHomeKey
        if (!automaticHandoff || !isDefaultLauncher()) goHome()
    }

    companion object {
        private const val ONBOARDING_LAUNCHER_REQUEST = "onboardingLauncherRequest"

        val Factory = viewModelFactory {
            initializer { AppNavigationViewModel(createSavedStateHandle()) }
        }
    }
}
