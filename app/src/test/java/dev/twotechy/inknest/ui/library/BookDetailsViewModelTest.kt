package dev.twotechy.inknest.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.repository.BookDetailsRepository
import dev.twotechy.inknest.model.BookDetails
import dev.twotechy.inknest.model.ReadingItemId
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailsViewModelTest {
    @Test fun selectionRestoresExpansionAndPaginationButNewBookResetsThem() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val requests = mutableListOf<Pair<ReadingItemId, Int>>()
            val repository = object : BookDetailsRepository {
                override fun observe(id: ReadingItemId, historyLimit: Int) =
                    MutableStateFlow(BookDetails()).also {
                        requests +=
                            id to historyLimit
                    }
                override fun retry() {}
            }
            val clock = object : StatisticsClock {
                override val moments =
                    MutableStateFlow(StatisticsMoment(LocalDate.of(2026, 11, 16), ZoneId.of("UTC")))
            }
            val handle = SavedStateHandle()
            val first = BookDetailsViewModel(repository, handle, clock)
            store.put("first", first)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                first.uiState.collect()
            }
            val id = ReadingItemId("source", "primary:Books/one.epub")
            first.select(id)
            first.toggleDescription()
            first.toggleHistory()
            first.moreHistory()
            runCurrent()
            assertEquals(id to 60, requests.last())
            val restored = BookDetailsViewModel(repository, handle, clock)
            store.put("restored", restored)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                restored.uiState.collect()
            }
            restored.select(id)
            runCurrent()
            assertTrue(restored.uiState.value.descriptionExpanded)
            assertTrue(restored.uiState.value.historyExpanded)
            assertEquals(id to 60, requests.last())
            val another = id.copy(path = "primary:Books/two.epub")
            restored.select(another)
            runCurrent()
            assertFalse(restored.uiState.value.descriptionExpanded)
            assertFalse(restored.uiState.value.historyExpanded)
            assertEquals(another to 30, requests.last())
            restored.select(null)
            runCurrent()
            assertEquals(null, restored.uiState.value.details.book)
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
