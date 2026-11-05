package dev.twotechy.inknest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val readingRepository: ReadingRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val readingStatsRepository: ReadingStatsRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        readingRepository.reading,
        installedAppsRepository.pinnedApps,
        readingStatsRepository.stats,
        readingRepository.sync,
    ) { reading, apps, stats, sync ->
        val readingSection = reading.toReadingSectionState()
        val homePins = apps.map { HomePinUiModel(it.componentId, it.label, it.iconVersion) }
        val homeStats = stats?.let { HomeReadingStats(it.todayMinutes, it.weekMinutes) }
        HomeUiState.Loaded(
            reading = readingSection,
            pinnedApps = homePins,
            stats = homeStats,
            sync = sync,
        )
    }.catch {
        emit(
            HomeUiState.Loaded(
                reading = ReadingSectionState.Error(
                    ReadingSyncIssue.Cache,
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
        viewModelScope.launch {
            try {
                readingRepository.refresh()
            } catch (_: IOException) {
                // Shared repository sync state contains the error; cached content stays visible.
            } finally {
                readingStatsRepository.refresh()
            }
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
