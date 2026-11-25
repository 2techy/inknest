package dev.twotechy.inknest.data.repository.statistics

import dev.twotechy.inknest.data.repository.reading.ReadingStatsRepository
import dev.twotechy.inknest.model.StatisticsData
import dev.twotechy.inknest.model.StatisticsDayDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface StatisticsRepository : ReadingStatsRepository {
    val data: StateFlow<StatisticsData>
    fun observeDay(day: LocalDate, page: Int): Flow<StatisticsDayDetail>
    fun retryDay()
    override suspend fun refresh()
}
