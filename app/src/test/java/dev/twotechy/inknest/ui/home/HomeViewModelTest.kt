package dev.twotechy.inknest.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.preview.HomePreviewController
import dev.twotechy.inknest.data.preview.HomePreviewMode
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
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
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        preview: HomePreviewController? = null,
    ) = HomeViewModel(
        reading,
        object : InstalledAppsRepository {
            override val pinnedApps = pins
        },
        object : ReadingStatsRepository {
            override val stats = this@HomeViewModelTest.stats
        },
        handle,
        preview,
    ).also { store.put("home", it) }

    @Test
    fun `maps loading empty error and content without inventing missing data`() =
        runTest(dispatcher) {
            val vm = viewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            runCurrent()
            assertEquals(HomeUiState.Loading, vm.uiState.value)
            reading.state.value = ReadingState.Empty
            runCurrent()
            assertEquals(HomeUiState.Empty, vm.uiState.value)
            reading.state.value = ReadingState.Error("No permission")
            runCurrent()
            assertEquals(HomeUiState.Error("No permission"), vm.uiState.value)
            reading.state.value = ReadingState.Content(book)
            runCurrent()
            val content = vm.uiState.value as HomeUiState.Content
            assertEquals(book.id, content.current.id)
            assertNull(content.current.author)
            assertNull(content.current.progress)
            assertNull(content.stats)
            assertTrue(content.pinnedApps.isEmpty())
        }

    @Test
    fun `combines independent book pins and statistics updates`() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        reading.state.value = ReadingState.Content(book)
        pins.value = listOf(PinnedApp("test/reader", "Reader"))
        stats.value = ReadingStats(12, 60)
        runCurrent()
        val content = vm.uiState.value as HomeUiState.Content
        assertEquals(listOf("Reader"), content.pinnedApps)
        assertEquals(HomeReadingStats(12, 60), content.stats)
        pins.value = emptyList()
        stats.value = null
        reading.state.value = ReadingState.Content(book.copy(title = "Changed", progress = 30))
        runCurrent()
        val updated = vm.uiState.value as HomeUiState.Content
        assertEquals("Changed", updated.current.title)
        assertEquals(30, updated.current.progress)
        assertTrue(updated.pinnedApps.isEmpty())
        assertNull(updated.stats)
    }

    @Test
    fun `refresh coalesces requests retains content on failure and recovers`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            reading.refreshAction = {
                gate.await()
                error("source unavailable")
            }
            reading.state.value = ReadingState.Content(book)
            val vm = viewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            runCurrent()
            vm.refresh()
            vm.refresh()
            runCurrent()
            assertEquals(1, reading.refreshCalls)
            assertTrue((vm.uiState.value as HomeUiState.Content).isRefreshing)
            gate.complete(Unit)
            runCurrent()
            val stale = vm.uiState.value as HomeUiState.Content
            assertEquals(book.id, stale.current.id)
            assertFalse(stale.isRefreshing)
            assertNotNull(stale.refreshError)
            reading.refreshAction =
                { reading.state.value = ReadingState.Content(book.copy(progress = 70)) }
            vm.refresh()
            runCurrent()
            val recovered = vm.uiState.value as HomeUiState.Content
            assertEquals(2, reading.refreshCalls)
            assertEquals(70, recovered.current.progress)
            assertNull(recovered.refreshError)
        }

    @Test
    fun `cancelled refresh is not shown as an error and can be retried`() = runTest(dispatcher) {
        reading.state.value = ReadingState.Content(book)
        reading.refreshAction = { throw CancellationException("cancelled") }
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.refresh()
        runCurrent()
        val state = vm.uiState.value as HomeUiState.Content
        assertFalse(state.isRefreshing)
        assertNull(state.refreshError)
        reading.refreshAction = {}
        vm.refresh()
        runCurrent()
        assertEquals(2, reading.refreshCalls)
    }

    @Test
    fun `clearing ViewModel cancels running refresh`() = runTest(dispatcher) {
        var cancelled = false
        reading.refreshAction = {
            try {
                CompletableDeferred<Unit>().await()
            } finally {
                cancelled = true
            }
        }
        val vm = viewModel()
        vm.refresh()
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(cancelled)
    }

    @Test
    fun `upstream collection stops after last subscriber leaves`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(0, reading.subscriptions)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        runCurrent()
        assertEquals(1, reading.subscriptions)
        collector.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, reading.state.subscriptionCount.value)
    }

    @Test
    fun `saved preview mode is restored and controls the repository`() = runTest(dispatcher) {
        var selected: HomePreviewMode? = null
        val controller = object : HomePreviewController {
            override fun select(mode: HomePreviewMode) {
                selected = mode
            }
        }
        val handle = SavedStateHandle(mapOf("homePreviewMode" to "Error"))
        val vm = viewModel(handle, controller)
        assertEquals(HomePreviewMode.Error, selected)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.previewMode.collect() }
        vm.onPreviewModeChanged(HomePreviewMode.Empty)
        runCurrent()
        assertEquals(HomePreviewMode.Empty, selected)
        assertEquals(HomePreviewMode.Empty, vm.previewMode.value)
        assertEquals("Empty", handle.get<String>("homePreviewMode"))
    }

    @Test
    fun `no preview controls are exposed without a debug controller`() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.previewMode.collect() }
        vm.onPreviewModeChanged(HomePreviewMode.Content)
        runCurrent()
        assertNull(vm.previewMode.value)
    }

    private class TestReadingRepository : ReadingRepository {
        val state = MutableStateFlow<ReadingState>(ReadingState.Loading)
        var subscriptions = 0
        override val reading = state.onSubscription { subscriptions++ }
        var refreshCalls = 0
        var refreshAction: suspend () -> Unit = {}
        override suspend fun refresh() {
            refreshCalls++
            refreshAction()
        }
    }
}
