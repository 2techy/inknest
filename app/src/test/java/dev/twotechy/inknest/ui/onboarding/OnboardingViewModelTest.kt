package dev.twotechy.inknest.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import dev.twotechy.inknest.data.repository.TestUserPreferencesRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
        prefs.failure = IOException("disk unavailable")
        vm.back()
        runCurrent()
        assertTrue(vm.error.value)
        assertFalse(prefs.appPreferences.value.onboardingCompleted)
        assertEquals(0, vm.completion.value)
        prefs.failure = null
        vm.finish()
        runCurrent()
        assertFalse(vm.error.value)
        assertTrue(prefs.appPreferences.value.onboardingCompleted)
        assertEquals(1, vm.completion.value)
        assertEquals(listOf("reader/Activity"), prefs.pinnedComponentIds.value)
    }

    @Test fun `step restores and manual review keeps preferences`() = runTest(dispatcher) {
        val prefs = TestUserPreferencesRepository()
        val handle = SavedStateHandle(mapOf("setupStep" to 2))
        val vm = OnboardingViewModel(prefs, handle)
        assertEquals(2, vm.step.value)
        assertEquals(0, vm.completion.value)
        vm.back()
        assertEquals(1, vm.step.value)
        prefs.completeOnboarding()
        vm.restart()
        assertEquals(0, vm.step.value)
        assertTrue(prefs.appPreferences.value.onboardingCompleted)
    }
}
