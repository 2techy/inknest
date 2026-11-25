package dev.twotechy.inknest.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.statistics.StatisticsDashboard
import dev.twotechy.inknest.data.repository.statistics.StatisticsPeriod
import dev.twotechy.inknest.data.repository.statistics.StatisticsRepository
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.platform.clock.StatisticsClock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class StatsViewModel(
    private val repository: StatisticsRepository,
    private val saved: SavedStateHandle,
    clock: StatisticsClock,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val options = combine(
        saved.getStateFlow("stats.period", restoredPeriod().name),
        saved.getStateFlow("stats.unit", StatisticsUnit.Time.name),
    ) { period, unit ->
        StatsOptions(
            StatisticsUnit.entries.find { it.name == unit } ?: StatisticsUnit.Time,
            StatisticsPeriod.entries.find { it.name == period } ?: StatisticsPeriod.ThisWeek,
        )
    }
    val uiState = combine(repository.data, clock.moments, options) {
            data,
            clock,
            options,
        ->
        val today = clock.today
        val dashboard = data.updatedAt?.let {
            StatisticsDashboard(today, data.days.filterKeys { it <= today })
        }
        StatsUiState(
            options,
            data,
            dashboard?.let {
                StatsOverview(
                    it.average(options.unit),
                    it.streak(options.unit),
                    it.todayAmount,
                    StatisticsPeriod.entries.associateWith(it::total),
                    it.days,
                    it.today,
                    it.chart(options.period),
                )
            },
        )
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    private fun restoredPeriod(): StatisticsPeriod = when (saved.get<String>("stats.granularity")) {
        "Weeks" -> StatisticsPeriod.ThisMonth
        "Months" -> StatisticsPeriod.ThisYear
        "Years" -> StatisticsPeriod.AllTime
        else -> StatisticsPeriod.ThisWeek
    }

    fun period(period: StatisticsPeriod) {
        saved["stats.period"] = period.name
    }
    fun unit(unit: StatisticsUnit) {
        saved["stats.unit"] = unit.name
    }
    fun observeDay(day: LocalDate, page: Int) = repository.observeDay(day, page)

    fun retryDay() = repository.retryDay()

    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true || uiState.value.isRefreshing) return
        refreshJob = viewModelScope.launch { repository.refresh() }
    }
}
