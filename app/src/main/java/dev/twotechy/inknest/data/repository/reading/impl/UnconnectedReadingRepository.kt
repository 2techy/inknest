package dev.twotechy.inknest.data.repository.reading.impl

import dev.twotechy.inknest.data.repository.reading.ReadingRepository
import dev.twotechy.inknest.data.repository.reading.ReadingState
import kotlinx.coroutines.flow.flowOf

// Production defaults until M2/M4/M8/M11 connect real sources. Never emit sample data.
internal class UnconnectedReadingRepository : ReadingRepository {
    override val reading = flowOf(ReadingState.Empty)
    override suspend fun refresh() = Unit
}
