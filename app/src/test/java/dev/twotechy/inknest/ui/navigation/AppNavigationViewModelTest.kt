package dev.twotechy.inknest.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationViewModelTest {
    private fun restoredApps() = AppNavigationViewModel(
        SavedStateHandle(mapOf("selectedDestination" to TopLevelDestination.Apps.name)),
    )

    @Test fun `initial HOME overrides a restored tab only once`() {
        val vm = restoredApps()
        vm.handleInitialLaunch(isHome = true, fromHistory = false)
        assertEquals(TopLevelDestination.Home.name, vm.selected.value)
        vm.select(TopLevelDestination.Apps)
        vm.handleInitialLaunch(isHome = true, fromHistory = false)
        assertEquals(TopLevelDestination.Apps.name, vm.selected.value)
    }

    @Test fun `recents and standalone launches keep the restored tab`() {
        for ((isHome, fromHistory) in listOf(true to true, false to false)) {
            val vm = restoredApps()
            vm.handleInitialLaunch(isHome = isHome, fromHistory = fromHistory)
            assertEquals(TopLevelDestination.Apps.name, vm.selected.value)
        }
    }

    @Test
    fun `HOME closes details and setup for the session`() {
        val vm = AppNavigationViewModel(SavedStateHandle())
        vm.offerSetup(false)
        assertEquals(true, vm.onboardingVisible.value)
        vm.openSettingsPage("ReadingData")
        vm.goHome()
        vm.offerSetup(false)
        assertEquals(false, vm.onboardingVisible.value)
        assertEquals("Overview", vm.settingsPage.value)
        assertEquals(TopLevelDestination.Home.name, vm.selected.value)
    }

    @Test
    fun `completed setup stays hidden until manually requested`() {
        val vm = AppNavigationViewModel(SavedStateHandle())
        vm.offerSetup(true)
        assertEquals(false, vm.onboardingVisible.value)
        vm.reviewSetup()
        assertEquals(true, vm.onboardingVisible.value)
        vm.select(TopLevelDestination.Apps)
        assertEquals(false, vm.onboardingVisible.value)
    }

    @Test
    fun `HOME bypass does not persist setup completion into a new session`() {
        val saved = SavedStateHandle(mapOf("onboardingVisible" to false, "setupOffered" to true))
        val vm = AppNavigationViewModel(saved)
        vm.offerSetup(false)
        assertEquals(true, vm.onboardingVisible.value)
    }

    @Test fun `restored picker keeps settings while a new HOME still resets navigation`() {
        val saved = SavedStateHandle(
            mapOf(
                "selectedDestination" to "Settings",
                "settingsPage" to "ReadingData",
            ),
        )
        val model = AppNavigationViewModel(saved)
        model.handleInitialLaunch(isHome = true, fromHistory = false, restoring = true)
        assertEquals("Settings", model.selected.value)
        assertEquals("ReadingData", model.settingsPage.value)
        model.goHome()
        assertEquals("Home", model.selected.value)
        assertEquals("Overview", model.settingsPage.value)
    }
}
