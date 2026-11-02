package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingItem
import kotlinx.coroutines.flow.Flow

sealed interface ReadingState {
    data object Loading : ReadingState
    data object Empty : ReadingState
    data class Error(val message: String) : ReadingState
    data class Content(val current: ReadingItem) : ReadingState
}

interface ReadingRepository {
    val reading: Flow<ReadingState>

    // Main-safe. A failed refresh must retain cached content; cancellation propagates.
    suspend fun refresh()
}
