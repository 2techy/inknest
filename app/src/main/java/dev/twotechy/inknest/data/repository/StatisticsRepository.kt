package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.StatisticsData
import kotlinx.coroutines.flow.StateFlow

interface StatisticsRepository : ReadingStatsRepository {
    val data: StateFlow<StatisticsData>
    override suspend fun refresh()
}
