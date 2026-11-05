package dev.twotechy.inknest.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.StatisticsAmount
import dev.twotechy.inknest.data.repository.StatisticsBucket
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.data.repository.StatisticsRepository
import dev.twotechy.inknest.data.repository.buildStatisticsDashboard
import dev.twotechy.inknest.data.repository.localStatisticsClock
import dev.twotechy.inknest.model.StatisticsData
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    private val dashboard = combine(
        repository.data.map { it.updatedAt to it.events }.distinctUntilChanged(),
        localStatisticsClock(),
    ) { (updated, events), (today, zone) ->
        (updated to events) to updated?.let { buildStatisticsDashboard(events, today, zone) }
    }.flowOn(Dispatchers.Default)

    val uiState = combine(repository.data, dashboard, options) { data, dashboard, options ->
        StatsUiState(
            options,
            data,
            dashboard.second?.takeIf { dashboard.first == (data.updatedAt to data.events) }?.let {
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
    }.flowOn(Dispatchers.Default)
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
