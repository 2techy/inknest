package dev.twotechy.inknest.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.data.repository.StatisticsRepository
import dev.twotechy.inknest.model.ReadingStats
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsData
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.platform.clock.StatisticsClock
import dev.twotechy.inknest.platform.clock.StatisticsMoment
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val today = LocalDate.parse("2026-11-05")
    private val clock = object : StatisticsClock {
        override val moments = MutableStateFlow(StatisticsMoment(today, ZoneId.of("UTC")))
    }
    private val repository = FakeStatisticsRepository(today)

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun cleanup() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun unitSwitchKeepsUnknownPagesAndNeverFetchesSources() = runTest(dispatcher) {
        repository.data.value = repository.data.value.copy(
            days = mapOf(today to StatisticsAmount(120, null)),
        )
        val saved = SavedStateHandle()
        val vm = StatsViewModel(repository, saved, clock, dispatcher)
        store.put("stats", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals(StatisticsUnit.Time, vm.uiState.value.options.unit)
        assertEquals(4.0, vm.uiState.value.overview!!.average)
        vm.unit(StatisticsUnit.Pages)
        runCurrent()
        val pages = vm.uiState.value.overview!!
        assertEquals(null, pages.average)
        assertEquals(null, pages.streak)
        assertEquals(null, pages.today.pages)
        assertEquals(null, pages.totals[StatisticsPeriod.AllTime]!!.pages)
        assertEquals(null, pages.activity[today]!!.pages)
        assertEquals(null, pages.chart.last().amount.pages)
        assertEquals(0, repository.refreshes)
        vm.unit(StatisticsUnit.Time)
        runCurrent()
        assertEquals(120L, vm.uiState.value.overview!!.today.seconds)
        assertEquals(4.0, vm.uiState.value.overview!!.average)
    }

    @Test fun calendarAndTimezoneChangesProjectLocalSummariesWithoutRefreshing() = runTest(
        dispatcher,
    ) {
        val vm = StatsViewModel(repository, SavedStateHandle(), clock, dispatcher)
        store.put("stats", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals(60L, vm.uiState.value.overview!!.today.seconds)
        assertEquals(180L, vm.uiState.value.overview!!.totals[StatisticsPeriod.AllTime]!!.seconds)
        clock.moments.value = StatisticsMoment(today.plusDays(1), ZoneId.of("UTC"))
        runCurrent()
        assertEquals(600L, vm.uiState.value.overview!!.today.seconds)
        assertEquals(780L, vm.uiState.value.overview!!.totals[StatisticsPeriod.AllTime]!!.seconds)
        val zone = ZoneId.of("America/New_York")
        clock.moments.value = StatisticsMoment(today, zone)
        repository.data.value = repository.data.value.copy(
            generation = "rebinned",
            zone = zone,
            days = mapOf(today to StatisticsAmount(180, 3)),
        )
        runCurrent()
        assertEquals(StatisticsAmount(180, 3), vm.uiState.value.overview!!.today)
        assertEquals(today, vm.uiState.value.overview!!.lastDay)
        assertEquals(0, repository.refreshes)
    }

    @Test fun choicesRestoreAndRefreshErrorsKeepTheExistingDashboard() = runTest(dispatcher) {
        val saved =
            SavedStateHandle(mapOf("stats.unit" to "Pages", "stats.granularity" to "Months"))
        val vm = StatsViewModel(repository, saved, clock, dispatcher)
        store.put("stats", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        runCurrent()
        assertEquals(StatisticsUnit.Pages, vm.uiState.value.options.unit)
        assertEquals(StatisticsGranularity.Months, vm.uiState.value.options.granularity)
        vm.granularity(StatisticsGranularity.Weeks)
        vm.unit(StatisticsUnit.Time)
        runCurrent()
        val previous = vm.uiState.value.overview
        vm.refresh()
        runCurrent()
        assertEquals(previous, vm.uiState.value.overview)
        assertTrue(vm.uiState.value.data.sync.stale)
        assertEquals(ReadingSyncIssue.Provider, vm.uiState.value.data.sync.error)
        assertEquals(1, repository.refreshes)
        assertEquals("Weeks", saved.get<String>("stats.granularity"))
        assertEquals("Time", saved.get<String>("stats.unit"))
    }
}

private class FakeStatisticsRepository(today: LocalDate) : StatisticsRepository {
    override val data = MutableStateFlow(
        StatisticsData(
            days = mapOf(
                today.minusDays(1) to StatisticsAmount(120, 2),
                today to StatisticsAmount(60, 1),
                today.plusDays(1) to StatisticsAmount(600, 10),
            ),
            generation = "initial",
            zone = ZoneId.of("UTC"),
            configured = true,
            updatedAt = 1,
        ),
    )
    override val stats = flowOf<ReadingStats?>(null)
    var refreshes = 0
    override suspend fun refresh() {
        refreshes++
        data.value =
            data.value.copy(
                sync = ReadingSyncStatus(error = ReadingSyncIssue.Provider, stale = true),
            )
    }
}
