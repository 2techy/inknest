package dev.twotechy.inknest.data.repository.statistics

import dev.twotechy.inknest.model.StatisticsAmount
import java.time.LocalDate

data class StatisticsBucket(
    val date: LocalDate,
    val amount: StatisticsAmount,
    val lastDay: LocalDate = date,
)
