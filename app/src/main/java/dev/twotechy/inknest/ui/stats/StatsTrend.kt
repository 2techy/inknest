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
            Text(statsUnitLabel(options.unit), style = MaterialTheme.typography.bodySmall)
        }
        StatsPeriods(options.period, onPeriod)
        val buckets = overview.chart
        if (buckets.isNotEmpty()) {
            val locale = currentUiLocale()
            val firstDate = buckets.first().date
            val lastDate = buckets.last().lastDay
            val firstPattern = when {
                options.period == StatisticsPeriod.AllTime -> "yyyy"
                firstDate.year != lastDate.year -> "dd MMM yyyy"
                firstDate.month != lastDate.month -> "dd MMM"
                else -> "dd"
            }
            val lastPattern = if (options.period ==
                StatisticsPeriod.AllTime
            ) {
                "yyyy"
            } else {
                "dd MMM yyyy"
            }
            val first = firstDate.format(DateTimeFormatter.ofPattern(firstPattern, locale))
            val last = lastDate.format(DateTimeFormatter.ofPattern(lastPattern, locale))
            Text(
                if (first ==
                    last
                ) {
                    first
                } else {
                    stringResource(R.string.stats_date_range, first, last)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        StatsChart(buckets, overview.lastDay, options, onDay)
    }
}
