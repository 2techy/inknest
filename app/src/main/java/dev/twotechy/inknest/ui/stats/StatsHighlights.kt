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
import androidx.compose.ui.text.style.TextAlign
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.formatStatsAmount
import dev.twotechy.inknest.ui.components.formatStatsAverage
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun StatsHighlights(overview: StatsOverview, unit: StatisticsUnit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
        StatsMetric(
            if (unit ==
                StatisticsUnit.Time
            ) {
                R.string.stats_min_today
            } else {
                R.string.stats_pages_today
            },
            formatStatsAmount(overview.today, unit),
            Modifier.weight(1f),
        )
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
}

@Composable
private fun StatsMetric(label: Int, value: String, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
    ) {
        Text(
            value,
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(label),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
