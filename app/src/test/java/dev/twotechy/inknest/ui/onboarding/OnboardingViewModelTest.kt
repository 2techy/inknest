package dev.twotechy.inknest.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.TestUserPreferencesRepository
import dev.twotechy.inknest.data.repository.apps.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.apps.impl.UnconnectedInstalledAppsRepository
import dev.twotechy.inknest.data.repository.sources.BookFolderConflictException
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.sources.SourceTreeSelection
import dev.twotechy.inknest.data.repository.sources.impl.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.KOREADER_PACKAGE
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Test fun requestPersistsCheckpointBeforeLaunchAndRestoredStepCanFinish() = runTest(
        dispatcher,
    ) {
        val prefs = TestUserPreferencesRepository()
        val beforeRestart = OnboardingViewModel(prefs, SavedStateHandle(mapOf("setupStep" to 2)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            beforeRestart.uiState.collect()
        }
        prefs.failure = IOException("disk unavailable")
        assertFalse(beforeRestart.prepareLauncherRequest())
        assertFalse(prefs.appPreferences.value.onboardingLauncherPending)
        prefs.failure = null
        assertTrue(beforeRestart.prepareLauncherRequest())
        assertTrue(prefs.appPreferences.value.onboardingLauncherPending)

        val restored = OnboardingViewModel(prefs, SavedStateHandle())
        restored.restoreLauncherStep()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            restored.uiState.collect()
        }
        runCurrent()
        assertEquals(2, restored.uiState.value.step)
        assertFalse(restored.uiState.value.finished)
        restored.discardLauncherCheckpoint()
        runCurrent()
        assertFalse(prefs.appPreferences.value.onboardingLauncherPending)
        assertEquals(2, restored.uiState.value.step)
        assertTrue(restored.prepareLauncherRequest())
        restored.finish()
        runCurrent()
        assertTrue(restored.uiState.value.finished)
        assertFalse(prefs.appPreferences.value.onboardingLauncherPending)
    }

    @Test fun readerDetectionTracksCatalogWithoutWritingConfiguration() = runTest(dispatcher) {
        val apps = OnboardingApps()
        val sources = OnboardingSources()
        val vm =
            OnboardingViewModel(TestUserPreferencesRepository(), SavedStateHandle(), sources, apps)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals(KoreaderDetection.Checking, vm.uiState.value.koreaderDetection)
        apps.isLoading.value = false
        apps.error.value = "Catalog read failed"
        runCurrent()
        assertEquals(KoreaderDetection.Failed, vm.uiState.value.koreaderDetection)
        apps.error.value = null
        apps.installedApps.value = listOf(AppInfo("$KOREADER_PACKAGE.other/Main", "KOReader"))
        runCurrent()
        assertEquals(KoreaderDetection.NotFound, vm.uiState.value.koreaderDetection)
        apps.installedApps.value = listOf(AppInfo("$KOREADER_PACKAGE/Main", "KOReader"))
        runCurrent()
        assertEquals(KoreaderDetection.Found, vm.uiState.value.koreaderDetection)
        vm.requestFolder(SourceFolder.KOReader)
        vm.dismissFolder()
        runCurrent()
        assertTrue(sources.selections.isEmpty())
        apps.installedApps.value = emptyList()
        runCurrent()
        assertEquals(KoreaderDetection.NotFound, vm.uiState.value.koreaderDetection)
    }

    @Test fun pairingRequiresReaderTreeAndAvailableApp() = runTest(dispatcher) {
        val apps = OnboardingApps()
        val sources = OnboardingSources()
        val vm =
            OnboardingViewModel(TestUserPreferencesRepository(), SavedStateHandle(), sources, apps)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        apps.installedApps.value = listOf(AppInfo("$KOREADER_PACKAGE/Main", "KOReader"))
        runCurrent()
        vm.acceptFolder(SourceFolder.Books, "books", 1)
        runCurrent()
        vm.acceptFolder(SourceFolder.KOReader, "reader", 1)
        runCurrent()
        apps.installedApps.value = emptyList()
        runCurrent()
        vm.acceptFolder(SourceFolder.KOReader, "reader-without-app", 1)
        runCurrent()
        assertEquals(listOf(false, true, false), sources.selections.map { it.pairKoreader })
    }

    @Test fun bookFolderShowsSavingOnlyUntilSaveCompletes() = runTest(dispatcher) {
        val sources = OnboardingSources()
        val vm = OnboardingViewModel(TestUserPreferencesRepository(), SavedStateHandle(), sources)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        val save = CompletableDeferred<Unit>()
        sources.selectionResult = save
        vm.acceptFolder(SourceFolder.Books, "books", 1)
        runCurrent()
        assertTrue(vm.uiState.value.bookFolderStatus.saving)

        save.complete(Unit)
        runCurrent()
        assertFalse(vm.uiState.value.bookFolderStatus.saving)
        assertEquals(null, vm.uiState.value.bookFolderStatus.errorMessage)
    }

    @Test fun overlappingBooksFolderShowsOnlyAnInlineError() = runTest(dispatcher) {
        val sources = OnboardingSources()
        val vm = OnboardingViewModel(TestUserPreferencesRepository(), SavedStateHandle(), sources)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        val save = CompletableDeferred<Unit>()
        sources.selectionResult = save
        vm.acceptFolder(SourceFolder.Books, "nested", 1)
        runCurrent()
        save.completeExceptionally(BookFolderConflictException())
        runCurrent()
        assertFalse(vm.uiState.value.bookFolderStatus.saving)
        assertEquals(R.string.book_folder_conflict, vm.uiState.value.bookFolderStatus.errorMessage)
        assertEquals(null, vm.uiState.value.errorMessage)
    }

    private class OnboardingApps : InstalledAppsRepository by UnconnectedInstalledAppsRepository() {
        override val installedApps = MutableStateFlow(emptyList<AppInfo>())
        override val isLoading = MutableStateFlow(true)
        override val error = MutableStateFlow<String?>(null)
    }

    private class OnboardingSources :
        SourceSettingsRepository by UnconnectedSourceSettingsRepository() {
        val selections = mutableListOf<SourceTreeSelection>()
        var selectionResult: CompletableDeferred<Unit>? = null

        override suspend fun connectTree(selection: SourceTreeSelection) {
            selections += selection
            selectionResult?.await()
        }
    }
}
