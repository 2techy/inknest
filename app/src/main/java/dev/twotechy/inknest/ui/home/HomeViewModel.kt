package dev.twotechy.inknest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingOpenRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.data.repository.StatisticsRepository
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.platform.clock.StatisticsClock
import dev.twotechy.inknest.ui.reading.ReadingOpenStateHolder
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val readingRepository: ReadingRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val readingStatsRepository: ReadingStatsRepository,
    openingRepository: ReadingOpenRepository,
    statisticsRepository: StatisticsRepository,
    clock: StatisticsClock,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    val bookOpening = ReadingOpenStateHolder(openingRepository, viewModelScope)
    private val refreshing = MutableStateFlow(false)
    private var refreshJob: Job? = null

    private data class RefreshState(val running: Boolean, val appsFailed: Boolean)
    private val refreshState = combine(
        refreshing,
        installedAppsRepository.isLoading,
        installedAppsRepository.error,
    ) { running, loading, error -> RefreshState(running || loading, error != null) }

    private val activity = combine(statisticsRepository.data, clock.moments) { data, moment ->
        homeActivity(data, moment.today)
    }.flowOn(defaultDispatcher)

    val uiState: StateFlow<HomeUiState> = combine(
        readingRepository.reading,
        installedAppsRepository.pinnedApps,
        readingStatsRepository.stats,
        readingRepository.sync,
        refreshState,
    ) { reading, apps, stats, sync, refresh ->
        val readingSection = reading.toReadingSectionState()
        val homePins = apps.map { HomePinUiModel(it.componentId, it.label, it.iconVersion) }
        val homeStats = stats?.let { HomeReadingStats(it.todayMinutes, it.streakDays) }
        HomeUiState.Loaded(
            reading = readingSection,
            pinnedApps = homePins,
            stats = homeStats,
            sync = sync,
            isRefreshing = refresh.running || sync.running,
            appsRefreshFailed = refresh.appsFailed,
        )
    }.combine(activity) { content, activity ->
        content.copy(activity = activity)
    }.combine(bookOpening.state) { content, opening ->
        content.copy(opening = opening)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun refreshApps() {
        viewModelScope.launch { installedAppsRepository.reload() }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshing.value = true
            try {
                coroutineScope {
                    launch { refreshReading() }
                    launch { readingStatsRepository.refresh() }
                    launch { installedAppsRepository.reload() }
                }
            } finally {
                refreshing.value = false
            }
        }
    }

    private suspend fun refreshReading() {
        try {
            readingRepository.refresh()
        } catch (_: IOException) {
            // Repository sync state retains cached data and carries the error.
        }
    }
}

private fun ReadingState.toReadingSectionState(): ReadingSectionState = when (this) {
    ReadingState.Loading -> ReadingSectionState.Loading

    ReadingState.Empty -> ReadingSectionState.Empty

    ReadingState.NotConfigured -> ReadingSectionState.NotConfigured

    is ReadingState.Error -> ReadingSectionState.Error(issue)

    is ReadingState.Content -> ReadingSectionState.Content(
        current = BookUiModel(
            current.id,
            current.title,
            current.author,
            current.progress,
            coverUri = current.coverUri,
            availability = current.availability,
        ),
    )
}
