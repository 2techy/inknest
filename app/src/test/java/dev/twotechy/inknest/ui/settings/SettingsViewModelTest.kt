package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.data.repository.AcknowledgementsRepository
import dev.twotechy.inknest.data.repository.TestUserPreferencesRepository
import dev.twotechy.inknest.data.repository.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    @Test fun `write failure preserves settings and retry keeps pins and source`() = runTest(
        dispatcher,
    ) {
        val prefs = TestUserPreferencesRepository()
        val sources = UnconnectedSourceSettingsRepository()
        val vm = SettingsViewModel(prefs, sources, AcknowledgementsRepository { "notices" })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        prefs.failure = IOException("disk unavailable")
        vm.setDisplayMode(DisplayMode.Standard)
        runCurrent()
        assertEquals(DisplayMode.EInk, vm.uiState.value.preferences!!.displayMode)
        assertEquals(SettingsError.Save, vm.uiState.value.error)
        assertFalse(vm.uiState.value.saving)
        prefs.failure = null
        vm.setDisplayMode(DisplayMode.Standard)
        runCurrent()
        vm.showPins(false)
        runCurrent()
        vm.showStats(false)
        runCurrent()
        assertEquals(DisplayMode.Standard, vm.uiState.value.preferences!!.displayMode)
        assertFalse(vm.uiState.value.preferences!!.showHomePinnedApps)
        assertFalse(vm.uiState.value.preferences!!.showHomeStats)
        assertNull(vm.uiState.value.error)
        assertEquals(listOf("reader/Activity"), prefs.pinnedComponentIds.value)
        assertEquals(sources.state.value, vm.uiState.value.source)
    }

    @Test fun `read errors preserve initial and last loaded state`() = runTest(dispatcher) {
        val snapshots = MutableSharedFlow<AppPreferences>(replay = 1)
        val readError = MutableStateFlow<String?>(null)
        val prefs = object : UserPreferencesRepository by TestUserPreferencesRepository() {
            override val appPreferences = snapshots
            override val readError = readError
        }
        val vm =
            SettingsViewModel(
                prefs,
                UnconnectedSourceSettingsRepository(),
                AcknowledgementsRepository {
                    ""
                },
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        readError.value = "unavailable"
        runCurrent()
        assertNull(vm.uiState.value.preferences)
        assertEquals(SettingsError.Read, vm.uiState.value.error)
        val saved = AppPreferences(displayMode = DisplayMode.Standard)
        snapshots.emit(saved)
        readError.value = null
        runCurrent()
        assertEquals(saved, vm.uiState.value.preferences)
        readError.value = "unavailable again"
        runCurrent()
        assertEquals(saved, vm.uiState.value.preferences)
        assertEquals(SettingsError.Read, vm.uiState.value.error)
    }

    @Test fun `acknowledgements can retry failed reads and reuse loaded content`() = runTest(
        dispatcher,
    ) {
        var reads = 0
        val repository = AcknowledgementsRepository {
            reads++
            if (reads == 1) throw IOException("unavailable")
            "license text"
        }
        val vm =
            SettingsViewModel(
                TestUserPreferencesRepository(),
                UnconnectedSourceSettingsRepository(),
                repository,
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.loadAcknowledgements()
        runCurrent()
        assertEquals(AcknowledgementsUiState.Failed, vm.uiState.value.acknowledgements)
        vm.loadAcknowledgements()
        runCurrent()
        assertEquals(
            AcknowledgementsUiState.Content("license text"),
            vm.uiState.value.acknowledgements,
        )
        vm.loadAcknowledgements()
        runCurrent()
        assertEquals(2, reads)
    }
}
