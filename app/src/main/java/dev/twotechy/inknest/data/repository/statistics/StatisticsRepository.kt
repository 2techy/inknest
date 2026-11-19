package dev.twotechy.inknest.data.repository.statistics

import dev.twotechy.inknest.data.repository.reading.ReadingStatsRepository
import dev.twotechy.inknest.model.StatisticsData
import kotlinx.coroutines.flow.StateFlow

interface StatisticsRepository : ReadingStatsRepository {
    val data: StateFlow<StatisticsData>
    override suspend fun refresh()
}
