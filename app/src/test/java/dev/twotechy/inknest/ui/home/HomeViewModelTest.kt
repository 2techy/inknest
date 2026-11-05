package dev.twotechy.inknest.ui.home

import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingStats
import kotlinx.coroutines.CancellationException
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val book = ReadingItem(ReadingItemId("test", "first.epub"), "First", null, null)
    private val reading = TestReadingRepository()
    private val pins = MutableStateFlow<List<PinnedApp>>(emptyList())
    private val stats = MutableStateFlow<ReadingStats?>(null)
    private var reloadApps: suspend () -> Unit = {}
    private var reloadStats: suspend () -> Unit = {}

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(
        reading,
        object : InstalledAppsRepository {
            override val installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
            override val pinnedApps = pins
            override val isLoading = MutableStateFlow(false)
            override suspend fun reload(retainPackages: Set<String>) = reloadApps()
            override suspend fun pin(componentId: String) = Unit
            override suspend fun unpin(componentId: String) = Unit
            override suspend fun movePin(componentId: String, offset: Int) = Unit
        },
        object : ReadingStatsRepository {
            override val stats = this@HomeViewModelTest.stats
            override suspend fun refresh() = reloadStats()
        },
    ).also { store.put("home", it) }

    @Test
    fun `pins remain available even when reading state is Error or Empty`() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        pins.value = listOf(PinnedApp("test/reader", "Reader"))
        reading.state.value =
            ReadingState.Error(dev.twotechy.inknest.model.ReadingSyncIssue.PermissionLost)
        runCurrent()

        val loaded = vm.uiState.value as HomeUiState.Loaded
        assertEquals(
            ReadingSectionState.Error(dev.twotechy.inknest.model.ReadingSyncIssue.PermissionLost),
            loaded.reading,
        )
        assertEquals(1, loaded.pinnedApps.size)
        assertEquals("test/reader", loaded.pinnedApps.first().componentId)
        assertEquals("Reader", loaded.pinnedApps.first().label)

        reading.state.value = ReadingState.Empty
        runCurrent()
        val emptyLoaded = vm.uiState.value as HomeUiState.Loaded
        assertEquals(ReadingSectionState.Empty, emptyLoaded.reading)
        assertEquals(1, emptyLoaded.pinnedApps.size)
    }

    @Test
    fun `refresh waits for all sources and allows retry after failure`() = runTest(dispatcher) {
        val appsDone = CompletableDeferred<Unit>()
        val statsDone = CompletableDeferred<Unit>()
        reloadApps = { appsDone.await() }
        reloadStats = { statsDone.await() }
        reading.refreshAction = { throw java.io.IOException("Unavailable") }
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.refresh()
        vm.refresh()
        runCurrent()
        assertEquals(1, reading.refreshCalls)
        assertTrue((vm.uiState.value as HomeUiState.Loaded).isRefreshing)
        appsDone.complete(Unit)
        runCurrent()
        assertTrue((vm.uiState.value as HomeUiState.Loaded).isRefreshing)
        statsDone.complete(Unit)
        runCurrent()
        assertFalse((vm.uiState.value as HomeUiState.Loaded).isRefreshing)
        vm.refresh()
        runCurrent()
        assertEquals(2, reading.refreshCalls)
        assertFalse((vm.uiState.value as HomeUiState.Loaded).isRefreshing)
    }

    private class TestReadingRepository : ReadingRepository {
        val state = MutableStateFlow<ReadingState>(ReadingState.Loading)
        override val reading = state
        var refreshCalls = 0
        var refreshAction: suspend () -> Unit = {}
        override suspend fun refresh() {
            refreshCalls++
            refreshAction()
        }
    }
}
