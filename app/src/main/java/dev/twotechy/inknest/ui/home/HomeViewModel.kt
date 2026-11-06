package dev.twotechy.inknest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val readingRepository: ReadingRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val readingStatsRepository: ReadingStatsRepository,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    private var refreshJob: Job? = null

    private data class RefreshState(val running: Boolean, val appsFailed: Boolean)
    private val refreshState = combine(
        refreshing,
        installedAppsRepository.isLoading,
        installedAppsRepository.error,
    ) { running, loading, error -> RefreshState(running || loading, error != null) }

    val uiState: StateFlow<HomeUiState> = combine(
        readingRepository.reading,
        installedAppsRepository.pinnedApps,
        readingStatsRepository.stats,
        readingRepository.sync,
        refreshState,
    ) { reading, apps, stats, sync, refresh ->
        val readingSection = reading.toReadingSectionState()
        val homePins = apps.map { HomePinUiModel(it.componentId, it.label, it.iconVersion) }
        val homeStats = stats?.let { HomeReadingStats(it.todayMinutes, it.weekMinutes) }
        HomeUiState.Loaded(
            reading = readingSection,
            pinnedApps = homePins,
            stats = homeStats,
            sync = sync,
            isRefreshing = refresh.running || sync.running,
            appsRefreshFailed = refresh.appsFailed,
        )
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
