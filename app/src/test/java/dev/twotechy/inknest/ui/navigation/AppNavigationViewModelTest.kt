package dev.twotechy.inknest.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationViewModelTest {
    private fun restoredApps() = AppNavigationViewModel(
        SavedStateHandle(mapOf("selectedDestination" to TopLevelDestination.Apps.name)),
    )

    @Test fun `HOME before UI subscribes is retained`() {
        val vm = restoredApps()
        vm.goHome()
        assertEquals(TopLevelDestination.Home.name, vm.selected.value)
    }

    @Test fun `new HOME intent overrides restored tab after process death`() {
        val vm = restoredApps()
        vm.handleInitialLaunch(isHome = true, fromHistory = false)
        assertEquals(TopLevelDestination.Home.name, vm.selected.value)
    }

    @Test fun `configuration recreation does not replay the initial HOME intent`() {
        val vm = restoredApps()
        vm.handleInitialLaunch(isHome = true, fromHistory = false)
        vm.select(TopLevelDestination.Apps)
        vm.handleInitialLaunch(isHome = true, fromHistory = false)
        assertEquals(TopLevelDestination.Apps.name, vm.selected.value)
    }

    @Test fun `recents restores the tab instead of replaying old HOME intent`() {
        val vm = restoredApps()
        vm.handleInitialLaunch(isHome = true, fromHistory = true)
        assertEquals(TopLevelDestination.Apps.name, vm.selected.value)
    }

    @Test fun `standalone launcher entry keeps the restored tab`() {
        val vm = restoredApps()
        vm.handleInitialLaunch(isHome = false, fromHistory = false)
        assertEquals(TopLevelDestination.Apps.name, vm.selected.value)
    }
}
