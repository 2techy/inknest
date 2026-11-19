package dev.twotechy.inknest.data.repository.reading

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncIssue

sealed interface ReadingState {
    data object Loading : ReadingState
    data object Empty : ReadingState
    data object NotConfigured : ReadingState
    data class Error(val issue: ReadingSyncIssue) : ReadingState
    data class Content(val current: ReadingItem) : ReadingState
}
