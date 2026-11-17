package dev.twotechy.inknest.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.repository.BookDetailsRepository
import dev.twotechy.inknest.model.BookDetails
import dev.twotechy.inknest.model.BookReadingStatistics
import dev.twotechy.inknest.model.BookStatisticsStatus
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.platform.clock.StatisticsClock
import dev.twotechy.inknest.platform.clock.StatisticsMoment
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailsViewModelTest {
    private val store = ViewModelStore()
    private val id = ReadingItemId("source", "primary:Books/one.epub")

    @After fun close() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun selectionRestoresPaginationAndNewBooksResetIt() = runTest {
        val source = PageRepository()
        val handle = SavedStateHandle(mapOf("detailsHistoryLimit" to 60))
        val first = model(source, handle)
        assertNull(handle.get<Int>("detailsHistoryLimit"))
        first.select(id)
        runCurrent()
        source.ready()
        runCurrent()
        first.toggleDescription()
        first.toggleHistory()
        first.historyPage(1)
        runCurrent()
        assertEquals(id to 1, source.requests.last())
        assertTrue(first.uiState.value.details.statistics.historyLoading)
        assertEquals(0, first.uiState.value.details.statistics.historyPage)
        source.ready()
        runCurrent()
        val restored = model(source, handle)
        restored.select(id)
        runCurrent()
        source.ready()
        runCurrent()
        assertTrue(restored.uiState.value.descriptionExpanded)
        assertTrue(restored.uiState.value.historyExpanded)
        assertEquals(1, restored.uiState.value.details.statistics.historyPage)
        restored.select(id.copy(path = "primary:Books/two.epub"))
        runCurrent()
        assertEquals(0, source.requests.last().second)
        assertFalse(restored.uiState.value.descriptionExpanded)
        assertFalse(restored.uiState.value.historyExpanded)
        restored.select(null)
        runCurrent()
        assertNull(restored.uiState.value.details.book)
    }

    @Test fun failedPageKeepsSnapshotAndLateResponsesCannotCrossBooks() = runTest {
        val source = PageRepository()
        val vm = model(source)
        vm.select(id)
        runCurrent()
        source.ready()
        runCurrent()
        vm.historyPage(1)
        runCurrent()
        val requests = source.requests.size
        vm.historyPage(2)
        runCurrent()
        assertEquals(requests, source.requests.size)
        source.response.value = source.response.value.copy(
            statistics = BookReadingStatistics(error = ReadingSyncIssue.Cache),
        )
        runCurrent()
        assertEquals(0, vm.uiState.value.details.statistics.historyPage)
        assertEquals(ReadingSyncIssue.Cache, vm.uiState.value.details.statistics.error)
        assertFalse(vm.uiState.value.details.statistics.historyLoading)
        vm.retry()
        assertEquals(1, source.retries)
        source.ready()
        runCurrent()
        assertEquals(1, vm.uiState.value.details.statistics.historyPage)
        val obsolete = source.response
        val other = id.copy(path = "primary:Books/other.epub")
        vm.select(other)
        runCurrent()
        obsolete.value = BookDetails(book = ReadingItem(id, "Obsolete", null, null))
        source.ready()
        runCurrent()
        assertEquals(other, vm.uiState.value.details.book?.id)
    }

    @Test fun refreshedTotalsClampAndPersistTheCommittedPage() = runTest {
        val source = PageRepository()
        val handle = SavedStateHandle(
            mapOf(
                "detailsIdentity" to arrayListOf(id.sourceId, id.path),
                "detailsHistoryPage" to 7,
            ),
        )
        val vm = model(source, handle)
        source.ready(page = 1, days = 6)
        runCurrent()
        assertEquals(1, handle.get<Int>("detailsHistoryPage"))
        assertEquals(1, source.requests.last().second)
        source.ready(page = 1, days = 6)
        runCurrent()
        assertEquals(1, vm.uiState.value.details.statistics.historyPage)
        source.ready(page = 0, days = 0)
        runCurrent()
        source.ready(page = 0, days = 0)
        runCurrent()
        assertEquals(0, handle.get<Int>("detailsHistoryPage"))
        assertEquals(1, vm.uiState.value.details.statistics.historyPageCount)
    }

    private fun TestScope.model(
        source: PageRepository,
        handle: SavedStateHandle = SavedStateHandle(),
    ): BookDetailsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val clock = object : StatisticsClock {
            override val moments =
                MutableStateFlow(StatisticsMoment(LocalDate.of(2026, 11, 16), ZoneId.of("UTC")))
        }
        val vm = BookDetailsViewModel(source, handle, clock)
        store.put("model-${source.requests.size}", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        return vm
    }
}

private class PageRepository : BookDetailsRepository {
    val requests = mutableListOf<Pair<ReadingItemId, Int>>()
    lateinit var response: MutableStateFlow<BookDetails>
    var retries = 0
    override fun observe(id: ReadingItemId, historyPage: Int): MutableStateFlow<BookDetails> {
        requests += id to historyPage
        response = MutableStateFlow(BookDetails(book = ReadingItem(id, "Book", null, null)))
        return response
    }
    fun ready(page: Int = requests.last().second, days: Int = 12) {
        response.value = response.value.copy(
            statistics = BookReadingStatistics(
                status = BookStatisticsStatus.Ready,
                readingDays = days,
                historyPage = page,
                hasMore =
                    page < 2,
            ),
        )
    }
    override fun retry() {
        retries++
    }
}
