package dev.twotechy.inknest.ui.home

import dev.twotechy.inknest.model.ReadingItemId

data class BookUiModel(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverLabel: String? = null,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(
        val current: BookUiModel,
        val pinnedApps: List<String>,
        val stats: HomeReadingStats? = null,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : HomeUiState
}

data class HomeReadingStats(val todayMinutes: Int?, val weekMinutes: Int?)
