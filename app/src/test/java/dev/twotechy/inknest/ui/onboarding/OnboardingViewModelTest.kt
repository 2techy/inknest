package dev.twotechy.inknest.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import dev.twotechy.inknest.data.repository.TestUserPreferencesRepository
import java.io.IOException
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    @Test fun `skip waits for persisted completion and can retry after failure`() = runTest(
        dispatcher,
    ) {
        val prefs = TestUserPreferencesRepository()
        val vm = OnboardingViewModel(prefs, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        prefs.failure = IOException("disk unavailable")
        vm.back()
        runCurrent()
        assertTrue(vm.uiState.value.saveError)
        assertFalse(prefs.appPreferences.value.onboardingCompleted)
        assertFalse(vm.uiState.value.finished)
        prefs.failure = null
        vm.finish()
        runCurrent()
        assertFalse(vm.uiState.value.saveError)
        assertTrue(prefs.appPreferences.value.onboardingCompleted)
        assertTrue(vm.uiState.value.finished)
        assertEquals(listOf("reader/Activity"), prefs.pinnedComponentIds.value)
    }

    @Test fun `step restores and manual review keeps preferences`() = runTest(dispatcher) {
        val prefs = TestUserPreferencesRepository()
        val handle = SavedStateHandle(mapOf("setupStep" to 2))
        val vm = OnboardingViewModel(prefs, handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals(2, vm.uiState.value.step)
        assertFalse(vm.uiState.value.finished)
        vm.back()
        runCurrent()
        assertEquals(1, vm.uiState.value.step)
        prefs.completeOnboarding()
        vm.restart()
        runCurrent()
        assertEquals(0, vm.uiState.value.step)
        assertTrue(prefs.appPreferences.value.onboardingCompleted)
    }

    @Test fun restoredFinishedStateDoesNotNeedACompletionEvent() = runTest(dispatcher) {
        val prefs = TestUserPreferencesRepository()
        prefs.completeOnboarding()
        val vm = OnboardingViewModel(prefs, SavedStateHandle(mapOf("setupFinished" to true)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertTrue(vm.uiState.value.finished)
        vm.restart()
        runCurrent()
        assertFalse(vm.uiState.value.finished)
        assertTrue(vm.uiState.value.preferences!!.onboardingCompleted)
    }
}
