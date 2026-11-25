package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.statistics.StatisticsPeriod
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.theme.InkSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun StatsTrend(
    overview: StatsOverview,
    options: StatsOptions,
    onPeriod: (StatisticsPeriod) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val buckets = overview.chart
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.stats_trend),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            if (buckets.isNotEmpty()) {
                Text(
                    statsDateRange(buckets.first().date, buckets.last().lastDay, options.period),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        StatsPeriods(options.period, onPeriod)
        StatsChart(buckets, overview.lastDay, options, onDay)
    }
}

@Composable
private fun statsDateRange(
    firstDate: LocalDate,
    lastDate: LocalDate,
    period: StatisticsPeriod,
): String {
    val locale = currentUiLocale()
    val lastPattern = if (period == StatisticsPeriod.AllTime) "yyyy" else "dd MMM yyyy"
    val firstPattern = when {
        period == StatisticsPeriod.AllTime -> lastPattern
        firstDate.year != lastDate.year -> lastPattern
        firstDate.month != lastDate.month -> "dd MMM"
        else -> "dd"
    }
    val last = lastDate.format(DateTimeFormatter.ofPattern(lastPattern, locale))
    if (firstDate == lastDate) return last
    val first = firstDate.format(DateTimeFormatter.ofPattern(firstPattern, locale))
    return if (first == last) last else stringResource(R.string.stats_date_range, first, last)
}
