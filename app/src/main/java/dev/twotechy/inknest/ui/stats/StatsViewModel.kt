package dev.twotechy.inknest.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.StatisticsBucket
import dev.twotechy.inknest.data.repository.StatisticsDashboard
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.data.repository.StatisticsRepository
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsData
import dev.twotechy.inknest.model.StatisticsGranularity
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

internal data class StatsOverview(
    val average: Double?,
    val streak: Int?,
    val today: StatisticsAmount,
    val totals: Map<StatisticsPeriod, StatisticsAmount>,
    val activity: Map<LocalDate, StatisticsAmount>,
    val lastDay: LocalDate,
    val chart: List<StatisticsBucket>,
)

internal data class StatsOptions(
    val unit: StatisticsUnit = StatisticsUnit.Time,
    val granularity: StatisticsGranularity = StatisticsGranularity.Days,
)

internal data class StatsUiState(
    val options: StatsOptions = StatsOptions(),
    val data: StatisticsData = StatisticsData(),
    val overview: StatsOverview? = null,
) {
    val isRefreshing: Boolean get() = data.sync.running
}

internal class StatsViewModel(
    private val repository: StatisticsRepository,
    private val saved: SavedStateHandle,
    clock: StatisticsClock,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val options = combine(
        saved.getStateFlow("stats.granularity", StatisticsGranularity.Days.name),
        saved.getStateFlow("stats.unit", StatisticsUnit.Time.name),
    ) { granularity, unit ->
        StatsOptions(
            StatisticsUnit.entries.find { it.name == unit } ?: StatisticsUnit.Time,
            StatisticsGranularity.entries.find { it.name == granularity }
                ?: StatisticsGranularity.Days,
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
                    it.chart(options.granularity),
                )
            },
        )
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun granularity(granularity: StatisticsGranularity) {
        saved["stats.granularity"] = granularity.name
    }
    fun unit(unit: StatisticsUnit) {
        saved["stats.unit"] = unit.name
    }
    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true || uiState.value.isRefreshing) return
        refreshJob = viewModelScope.launch { repository.refresh() }
    }
}
