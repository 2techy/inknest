package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingStats
import kotlinx.coroutines.flow.Flow

interface ReadingStatsRepository {
    val stats: Flow<ReadingStats?>
}
