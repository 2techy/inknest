package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Embedded
import androidx.room3.Relation

internal data class StatisticsSnapshot(
    @Embedded val source: StatisticsSourceEntity,
    @Relation(parentColumns = ["generation"], entityColumns = ["generation"])
    val days: List<StatisticsDayEntity>,
)
