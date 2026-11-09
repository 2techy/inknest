package dev.twotechy.inknest.ui.settings

import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.repository.AcknowledgementsRepository
import dev.twotechy.inknest.data.repository.TestUserPreferencesRepository
import dev.twotechy.inknest.data.repository.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.model.ThirdPartyLicense
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LicenseSettingsTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun cleanup() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun loadingFailuresAndRetriesAreIsolatedAndSuccessfulContentIsCached() = runTest(
        dispatcher,
    ) {
        val pending = CompletableDeferred<Unit>()
        val reads = mutableMapOf<ThirdPartyLicense, Int>()
        val vm = SettingsViewModel(
            TestUserPreferencesRepository(),
            UnconnectedSourceSettingsRepository(),
            AcknowledgementsRepository { license ->
                reads[license] = (reads[license] ?: 0) + 1
                if (license == ThirdPartyLicense.LuaJ && reads[license] == 1) {
                    pending.await()
                    throw IOException("unavailable")
                }
                license.name
            },
        )
        store.put("settings", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.loadAcknowledgements(ThirdPartyLicense.LuaJ)
        vm.loadAcknowledgements(ThirdPartyLicense.LuaJ)
        vm.loadAcknowledgements(ThirdPartyLicense.MaterialSymbols)
        runCurrent()
        assertEquals(
            AcknowledgementsUiState.Loading,
            vm.uiState.value.acknowledgements[ThirdPartyLicense.LuaJ],
        )
        val symbols = AcknowledgementsUiState.Content("MaterialSymbols")
        assertEquals(symbols, vm.uiState.value.acknowledgements[ThirdPartyLicense.MaterialSymbols])
        pending.complete(Unit)
        runCurrent()
        assertEquals(
            AcknowledgementsUiState.Failed,
            vm.uiState.value.acknowledgements[ThirdPartyLicense.LuaJ],
        )
        assertEquals(symbols, vm.uiState.value.acknowledgements[ThirdPartyLicense.MaterialSymbols])
        vm.loadAcknowledgements(ThirdPartyLicense.LuaJ)
        vm.loadAcknowledgements(ThirdPartyLicense.MaterialSymbols)
        runCurrent()
        assertEquals(
            AcknowledgementsUiState.Content("LuaJ"),
            vm.uiState.value.acknowledgements[ThirdPartyLicense.LuaJ],
        )
        assertEquals(
            mapOf(ThirdPartyLicense.LuaJ to 2, ThirdPartyLicense.MaterialSymbols to 1),
            reads,
        )
    }

    @Test fun licenseDetailsReturnToIndexAndIndexReturnsToAbout() {
        assertEquals(SettingsPage.Notices, SettingsPage.LuaJLicense.parent)
        assertEquals(SettingsPage.Notices, SettingsPage.MaterialSymbolsLicense.parent)
        assertEquals(SettingsPage.About, SettingsPage.Notices.parent)
        assertEquals(ThirdPartyLicense.LuaJ, SettingsPage.LuaJLicense.license)
        assertEquals(ThirdPartyLicense.MaterialSymbols, SettingsPage.MaterialSymbolsLicense.license)
    }
}
