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
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun StatsTotals(totals: Map<StatisticsPeriod, StatisticsAmount>, unit: StatisticsUnit) {
    Text(stringResource(R.string.stats_totals), style = MaterialTheme.typography.titleMedium)
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        StatisticsPeriod.entries.forEachIndexed { index, period ->
            if (index > 0) HorizontalDivider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg),
            ) {
                Text(
                    stringResource(period.label()),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    formatStatsAmount(totals[period] ?: StatisticsAmount(), unit),
                    Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun StatisticsPeriod.label() = when (this) {
    StatisticsPeriod.ThisWeek -> R.string.stats_this_week
    StatisticsPeriod.ThisMonth -> R.string.stats_this_month
    StatisticsPeriod.ThisYear -> R.string.stats_this_year
    StatisticsPeriod.AllTime -> R.string.stats_all_time
}
