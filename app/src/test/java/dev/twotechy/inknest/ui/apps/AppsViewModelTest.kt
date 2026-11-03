package dev.twotechy.inknest.ui.apps

import androidx.lifecycle.SavedStateHandle
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import java.io.IOException
import kotlinx.coroutines.CancellationException
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val installedAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val pinnedAppsFlow = MutableStateFlow<List<PinnedApp>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(true)

    private var pinFailure: Exception? = null
    private val moveCalls = mutableListOf<Pair<String, Int>>()

    private val pinnedCalls = mutableListOf<String>()
    private val unpinnedCalls = mutableListOf<String>()

    private val fakeRepo = object : InstalledAppsRepository {
        override val installedApps = installedAppsFlow
        override val pinnedApps = pinnedAppsFlow
        override val isLoading = isLoadingFlow

        override suspend fun reload(retainPackages: Set<String>) = Unit
        override suspend fun pin(componentId: String) {
            pinFailure?.let { throw it }
            pinnedCalls.add(componentId)
        }
        override suspend fun unpin(componentId: String) {
            unpinnedCalls.add(componentId)
        }
        override suspend fun movePin(componentId: String, offset: Int) {
            pinFailure?.let { throw it }
            moveCalls.add(componentId to offset)
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `search filters apps when search is active`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = AppsViewModel(fakeRepo, handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        installedAppsFlow.value = listOf(
            AppInfo("com.koreader/MainActivity", "KOReader"),
            AppInfo("com.android.settings/Settings", "Settings"),
            AppInfo("org.ebook.reader/ReaderActivity", "EBook Reader"),
        )
        pinnedAppsFlow.value = listOf(PinnedApp("com.koreader/MainActivity", "KOReader"))
        isLoadingFlow.value = false
        runCurrent()

        assertEquals(3, vm.uiState.value.allApps.size)
        assertTrue(vm.uiState.value.allApps[0].isPinned)
        assertFalse(vm.uiState.value.allApps[1].isPinned)

        // Activate search and query
        vm.onSearchActiveChanged(true)
        vm.onSearchQueryChanged("reader")
        runCurrent()

        val filtered = vm.uiState.value.allApps
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.label == "KOReader" })
        assertTrue(filtered.any { it.label == "EBook Reader" })

        // Deactivating search clears query and restores full list
        vm.onSearchActiveChanged(false)
        runCurrent()
        assertEquals(3, vm.uiState.value.allApps.size)
        assertEquals("", vm.uiState.value.searchQuery)
    }

    @Test
    fun `toggleViewMode alternates between Grid and List`() = runTest(dispatcher) {
        val vm = AppsViewModel(fakeRepo, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()

        assertEquals(AppsViewMode.Grid, vm.uiState.value.viewMode)

        vm.toggleViewMode()
        runCurrent()
        assertEquals(AppsViewMode.List, vm.uiState.value.viewMode)

        vm.toggleViewMode()
        runCurrent()
        assertEquals(AppsViewMode.Grid, vm.uiState.value.viewMode)
    }

    @Test
    fun `pin and unpin delegate to repository`() = runTest(dispatcher) {
        val vm = AppsViewModel(fakeRepo, SavedStateHandle())
        vm.pin("com.example/App")
        runCurrent()
        assertEquals(listOf("com.example/App"), pinnedCalls)

        vm.unpin("com.example/App")
        runCurrent()
        assertEquals(listOf("com.example/App"), unpinnedCalls)
    }

    @Test
    fun `failed write keeps current apps and can be retried without crashing`() =
        runTest(dispatcher) {
            val vm = AppsViewModel(fakeRepo, SavedStateHandle())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            installedAppsFlow.value = listOf(AppInfo("pkg.a/A", "Alpha"))
            isLoadingFlow.value = false
            pinFailure = IOException("Disk full")
            vm.pin("pkg.a/A")
            runCurrent()
            assertNotNull(vm.uiState.value.error)
            assertEquals(1, vm.uiState.value.allApps.size)
            assertTrue(vm.uiState.value.canDismissError)
            vm.dismissActionError()
            runCurrent()
            assertNull(vm.uiState.value.error)
            pinFailure = null
            vm.pin("pkg.a/A")
            runCurrent()
            assertEquals(listOf("pkg.a/A"), pinnedCalls)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `moving a pin uses stable identity and reports boundary actions`() = runTest(dispatcher) {
        val vm = AppsViewModel(fakeRepo, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        installedAppsFlow.value = listOf(AppInfo("a/A", "A"), AppInfo("b/B", "B"))
        pinnedAppsFlow.value = listOf(PinnedApp("b/B", "B"), PinnedApp("a/A", "A"))
        runCurrent()
        assertFalse(vm.uiState.value.pinnedApps.first().canMoveEarlier)
        assertTrue(vm.uiState.value.pinnedApps.first().canMoveLater)
        assertTrue(vm.uiState.value.pinnedApps.last().canMoveEarlier)
        assertFalse(vm.uiState.value.pinnedApps.last().canMoveLater)
        vm.movePin("b/B", 1)
        runCurrent()
        assertEquals(listOf("b/B" to 1), moveCalls)
    }

    @Test
    fun `cancelled pin operation does not become an error`() = runTest(dispatcher) {
        val vm = AppsViewModel(fakeRepo, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        pinFailure = CancellationException("cancelled")
        vm.pin("pkg.a/A")
        runCurrent()
        assertNull(vm.uiState.value.error)
    }
}
