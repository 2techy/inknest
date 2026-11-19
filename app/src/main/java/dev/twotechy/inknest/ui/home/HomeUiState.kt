package dev.twotechy.inknest.ui.home

import dev.twotechy.inknest.model.ReadingOpenState
import dev.twotechy.inknest.model.ReadingSyncStatus

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Loaded(
        val reading: ReadingSectionState,
        val pinnedApps: List<HomePinUiModel>,
        val stats: HomeReadingStats? = null,
        val sync: ReadingSyncStatus = ReadingSyncStatus(),
        val isRefreshing: Boolean = false,
        val appsRefreshFailed: Boolean = false,
        val activity: HomeReadingActivity? = null,
        val opening: ReadingOpenState = ReadingOpenState.Idle,
    ) : HomeUiState
}
