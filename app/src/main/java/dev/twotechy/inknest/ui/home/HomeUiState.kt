package dev.twotechy.inknest.ui.home

data class BookPreview(
    val id: String,
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
        val current: BookPreview,
        val pinnedApps: List<String>,
        val stats: HomeReadingStats? = null,
    ) : HomeUiState
}

data class HomeReadingStats(
    val todayMinutes: Int?,
    val weekMinutes: Int?,
)
