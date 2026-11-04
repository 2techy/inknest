package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed interface ReadingState {
    data object Loading : ReadingState
    data object Empty : ReadingState
    data object NotConfigured : ReadingState
    data class Error(val issue: ReadingSyncIssue) : ReadingState
    data class Content(val current: ReadingItem) : ReadingState
}

interface ReadingRepository {
    val reading: Flow<ReadingState>
    val sync: Flow<ReadingSyncStatus> get() = flowOf(ReadingSyncStatus())

    // Main-safe. A failed refresh must retain cached content; cancellation propagates.
    suspend fun refresh()
}
