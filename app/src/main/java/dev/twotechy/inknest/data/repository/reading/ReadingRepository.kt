package dev.twotechy.inknest.data.repository.reading

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ReadingRepository {
    val reading: Flow<ReadingState>
    val trackedItems: Flow<List<ReadingItem>> get() = flowOf(emptyList())
    val sync: Flow<ReadingSyncStatus> get() = flowOf(ReadingSyncStatus())

    // Main-safe. A failed refresh must retain cached content; cancellation propagates.
    suspend fun refresh()
}
