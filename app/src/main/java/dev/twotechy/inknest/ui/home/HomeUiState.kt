package dev.twotechy.inknest.ui.home

import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus

data class BookUiModel(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverUri: String? = null,
    val canContinue: Boolean = false,
)

data class HomePinUiModel(val componentId: String, val label: String, val iconVersion: Long = 0)

sealed interface ReadingSectionState {
    data object Loading : ReadingSectionState
    data object Empty : ReadingSectionState
    data object NotConfigured : ReadingSectionState
    data class Error(val issue: ReadingSyncIssue) : ReadingSectionState
    data class Content(val current: BookUiModel) : ReadingSectionState
}

data class HomeReadingStats(val todayMinutes: Int?, val weekMinutes: Int?)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Loaded(
        val reading: ReadingSectionState,
        val pinnedApps: List<HomePinUiModel>,
        val stats: HomeReadingStats? = null,
        val sync: ReadingSyncStatus = ReadingSyncStatus(),
    ) : HomeUiState
}
