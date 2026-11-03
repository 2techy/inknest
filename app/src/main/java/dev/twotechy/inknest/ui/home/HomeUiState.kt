package dev.twotechy.inknest.ui.home

import dev.twotechy.inknest.model.ReadingItemId

data class BookUiModel(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverLabel: String? = null,
)

data class HomePinUiModel(val componentId: String, val label: String, val iconVersion: Long = 0)

sealed interface ReadingSectionState {
    data object Loading : ReadingSectionState
    data object Empty : ReadingSectionState
    data class Error(val message: String) : ReadingSectionState
    data class Content(
        val current: BookUiModel,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : ReadingSectionState
}

data class HomeReadingStats(val todayMinutes: Int?, val weekMinutes: Int?)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Loaded(
        val reading: ReadingSectionState,
        val pinnedApps: List<HomePinUiModel>,
        val stats: HomeReadingStats? = null,
    ) : HomeUiState
}
