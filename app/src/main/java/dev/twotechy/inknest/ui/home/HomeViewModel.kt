package dev.twotechy.inknest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val readingRepository: ReadingRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    readingStatsRepository: ReadingStatsRepository,
) : ViewModel() {
    internal data class RefreshState(val running: Boolean = false, val error: String? = null)

    private val refreshState = MutableStateFlow(RefreshState())
    private var refreshJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        readingRepository.reading,
        installedAppsRepository.pinnedApps,
        readingStatsRepository.stats,
        refreshState,
    ) { reading, apps, stats, refresh ->
        val readingSection = reading.toReadingSectionState(refresh)
        val homePins = apps.map { HomePinUiModel(it.componentId, it.label, it.iconVersion) }
        val homeStats = stats?.let { HomeReadingStats(it.todayMinutes, it.weekMinutes) }
        HomeUiState.Loaded(
            reading = readingSection,
            pinnedApps = homePins,
            stats = homeStats,
        )
    }.catch {
        emit(
            HomeUiState.Loaded(
                reading = ReadingSectionState.Error(
                    "Reading data is unavailable. Please try again.",
                ),
                pinnedApps = emptyList(),
                stats = null,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun refreshApps() {
        viewModelScope.launch { installedAppsRepository.reload() }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshState.value = RefreshState(running = true)
            try {
                readingRepository.refresh()
                refreshState.value = RefreshState()
            } catch (cancelled: CancellationException) {
                refreshState.value = RefreshState()
                throw cancelled
            } catch (_: Exception) {
                refreshState.value =
                    RefreshState(error = "Unable to refresh books. Please try again.")
            }
        }
    }
}

private fun ReadingState.toReadingSectionState(
    refresh: HomeViewModel.RefreshState,
): ReadingSectionState = when (this) {
    ReadingState.Loading -> ReadingSectionState.Loading

    ReadingState.Empty -> ReadingSectionState.Empty

    is ReadingState.Error -> ReadingSectionState.Error(message)

    is ReadingState.Content -> ReadingSectionState.Content(
        current = BookUiModel(
            current.id,
            current.title,
            current.author,
            current.progress,
            current.coverUri,
        ),
        isRefreshing = refresh.running,
        refreshError = refresh.error,
    )
}
