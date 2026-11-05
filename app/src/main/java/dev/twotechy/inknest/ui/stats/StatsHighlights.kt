package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun StatsHighlights(overview: StatsOverview, unit: StatisticsUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
            StatsMetric(
                R.string.stats_daily_average,
                formatStatsAverage(overview.average, unit),
                Modifier.weight(1f),
            )
            StatsMetric(
                R.string.stats_streak,
                overview.streak?.let { stringResource(R.string.stats_days_value, it) }
                    ?: stringResource(R.string.stats_unknown),
                Modifier.weight(1f),
            )
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
            StatsMetric(
                R.string.stats_min_today,
                formatReadingTime(overview.today.seconds),
                Modifier.weight(1f),
            )
            StatsMetric(
                R.string.stats_pages_today,
                formatStatsAmount(overview.today, StatisticsUnit.Pages),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsMetric(label: Int, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
