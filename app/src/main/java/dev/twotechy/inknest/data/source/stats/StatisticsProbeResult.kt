package dev.twotechy.inknest.data.source.stats

import java.io.IOException

internal data class StatisticsProbeResult(
    val books: Long,
    val pageRows: Long,
    val walPresent: Boolean,
)
internal class UnsupportedStatisticsSchema : IOException()

internal class UnstableStatisticsSnapshot : IOException()
internal class InvalidStatisticsSnapshot : IOException()
