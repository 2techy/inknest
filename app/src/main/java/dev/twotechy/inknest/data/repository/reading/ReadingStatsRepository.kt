package dev.twotechy.inknest.data.repository.reading

import dev.twotechy.inknest.model.ReadingStatistics
import kotlinx.coroutines.flow.Flow

interface ReadingStatsRepository {
    val stats: Flow<ReadingStatistics?>
    suspend fun refresh() = Unit
}
