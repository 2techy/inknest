package dev.twotechy.inknest.ui.navigation

import androidx.lifecycle.SavedStateHandle
import dev.twotechy.inknest.model.ReadingItemId
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationViewModelTest {
    @Test fun bookOpenedFromStatsRestoresAndReturnsToItsDay() {
        val day = LocalDate.of(2026, 11, 24)
        val book = ReadingItemId("source", "primary:books/A, B.epub")
        val dayDetail = DetailDestination.StatsDay(day)
        val bookDetail = DetailDestination.Book(book)
        val handle = SavedStateHandle()
        val navigation = AppNavigationViewModel(handle)
        navigation.openStatsDay(day)
        assertEquals(emptyList<DetailDestination>(), navigation.detailStack.value)

        navigation.select(TopLevelDestination.Stats)
        navigation.openStatsDay(day)
        navigation.openBook(book)

        val restored = AppNavigationViewModel(
            SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }),
        )
        restored.handleInitialLaunch(isHome = true, fromHistory = false, restoring = true)
        restored.openStatsDay(day.plusDays(1))
        assertEquals(listOf(dayDetail, bookDetail), restored.detailStack.value)
        restored.backFromDetail()
        assertEquals(listOf(dayDetail), restored.detailStack.value)
        restored.backFromDetail()
        assertEquals(emptyList<DetailDestination>(), restored.detailStack.value)
        assertEquals(TopLevelDestination.Stats.name, restored.selected.value)

        restored.select(TopLevelDestination.Library)
        restored.openBook(book)
        restored.openStatsDay(day)
        assertEquals(listOf(bookDetail), restored.detailStack.value)
        assertEquals(TopLevelDestination.Library.name, restored.selected.value)
        restored.handleHomeIntent(fromHomeKey = true) { true }
        assertEquals(emptyList<DetailDestination>(), restored.detailStack.value)
        assertEquals(TopLevelDestination.Home.name, restored.selected.value)
    }

    @Test fun settingsReturnsToOriginAfterRecreationAndLegacyDefaultsToHome() {
        val handle = SavedStateHandle()
        val vm = AppNavigationViewModel(handle)
        vm.select(TopLevelDestination.Stats)
        vm.select(TopLevelDestination.Settings)
        vm.openSettingsPage("ReadingData")
        val restored = AppNavigationViewModel(
            SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }),
        )
        restored.leaveSettings()
        assertEquals(TopLevelDestination.Stats.name, restored.selected.value)
        assertEquals("Overview", restored.settingsPage.value)
        val legacy = AppNavigationViewModel(
            SavedStateHandle(mapOf("selectedDestination" to "Settings")),
        )
        legacy.leaveSettings()
        assertEquals(TopLevelDestination.Home.name, legacy.selected.value)
    }

    @Test fun librarySearchIsConsumedOnceAndHomeCancelsPendingRequest() {
        val vm = AppNavigationViewModel(SavedStateHandle())
        vm.searchLibrary()
        assertEquals(TopLevelDestination.Library.name, vm.selected.value)
        assertEquals(true, vm.consumeLibrarySearch())
        assertEquals(false, vm.consumeLibrarySearch())
        vm.searchLibrary()
        vm.goHome()
        assertEquals(false, vm.consumeLibrarySearch())
    }

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

    @Test fun launcherHandoffKeepsSetupOpenOnlyOnceAfterRestoration() {
        val saved = SavedStateHandle()
        val navigation = AppNavigationViewModel(saved)
        navigation.select(TopLevelDestination.Settings)
        navigation.reviewSetup()
        navigation.beginOnboardingLauncherRequest()

        val restored = AppNavigationViewModel(
            SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }),
        )
        restored.handleHomeIntent(fromHomeKey = false) { true }
        assertEquals(true, restored.onboardingVisible.value)
        assertEquals(TopLevelDestination.Settings.name, restored.selected.value)

        restored.handleHomeIntent(fromHomeKey = false) { true }
        assertEquals(false, restored.onboardingVisible.value)
        assertEquals(TopLevelDestination.Home.name, restored.selected.value)
    }

    @Test fun coldStartRestoresTheLauncherScreenForFirstRunAndReview() {
        for (completed in listOf(false, true)) {
            val navigation = AppNavigationViewModel(SavedStateHandle())
            var restored = false
            navigation.handleInitialLaunch(isHome = true, fromHistory = false)
            navigation.restoreLauncherSetup(completed) { restored = true }
            navigation.offerSetup(completed)

            assertEquals(true, restored)
            assertEquals(true, navigation.onboardingVisible.value)
            navigation.goHome()
            navigation.restoreLauncherSetup(completed) { error("Restore must run once per launch") }
            assertEquals(false, navigation.onboardingVisible.value)
        }
    }

    @Test fun homeKeyAndUnsuccessfulLauncherHandoffLeaveSetup() {
        for ((fromHomeKey, isDefaultLauncher) in listOf(true to true, false to false)) {
            val navigation = AppNavigationViewModel(SavedStateHandle())
            navigation.offerSetup(false)
            navigation.beginOnboardingLauncherRequest()

            navigation.handleHomeIntent(fromHomeKey) { isDefaultLauncher }

            assertEquals(false, navigation.onboardingVisible.value)
        }
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

    @Test fun `locale recreation retains Appearance without reopening completed setup`() {
        val model = AppNavigationViewModel(
            SavedStateHandle(
                mapOf(
                    "selectedDestination" to "Settings",
                    "settingsPage" to "Appearance",
                ),
            ),
        )
        model.handleInitialLaunch(isHome = true, fromHistory = false, restoring = true)
        model.offerSetup(true)
        assertEquals("Settings", model.selected.value)
        assertEquals("Appearance", model.settingsPage.value)
        assertEquals(false, model.onboardingVisible.value)
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

    @Test fun restoredFirstRunClosesButReviewRemainsWhenPreferencesAreComplete() {
        for (entry in SetupEntry.entries) {
            val saved = SavedStateHandle(
                mapOf(
                    "onboardingVisible" to true,
                    "setupEntry" to entry.name,
                ),
            )
            val navigation = AppNavigationViewModel(saved)
            navigation.offerSetup(true)
            assertEquals(entry == SetupEntry.Review, navigation.onboardingVisible.value)
        }
        val navigation = AppNavigationViewModel(SavedStateHandle())
        navigation.offerSetup(false)
        navigation.offerSetup(true)
        assertEquals(false, navigation.onboardingVisible.value)
    }

    @Test fun restoredFinishDoesNotOverrideNavigationAfterWizardClosed() {
        val navigation = restoredApps()
        navigation.finishSetup(true)
        assertEquals(TopLevelDestination.Apps.name, navigation.selected.value)
        navigation.reviewSetup()
        navigation.finishSetup(false)
        assertEquals(true, navigation.onboardingVisible.value)
        navigation.finishSetup(true)
        assertEquals(false, navigation.onboardingVisible.value)
        assertEquals(TopLevelDestination.Home.name, navigation.selected.value)
    }
}
