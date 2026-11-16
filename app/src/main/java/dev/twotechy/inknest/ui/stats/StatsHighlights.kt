package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.formatStatsAmount
import dev.twotechy.inknest.ui.components.formatStatsAverage
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun StatsHighlights(overview: StatsOverview, unit: StatisticsUnit) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        StatsMetric(
            R.drawable.ic_stats_book,
            R.string.stats_today,
            formatStatsAmount(overview.today, unit),
            Modifier.weight(1f),
        )
        StatsMetric(
            R.drawable.ic_stats_average,
            R.string.stats_daily_average,
            formatStatsAverage(overview.average, unit),
            Modifier.weight(1f),
        )
        StatsMetric(
            R.drawable.ic_streak,
            R.string.stats_current_streak,
            overview.streak?.let { stringResource(R.string.stats_days_value, it) }
                ?: stringResource(R.string.stats_unknown),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatsMetric(icon: Int, label: Int, value: String, modifier: Modifier) {
    Surface(
        modifier.fillMaxHeight(),
        shape = RoundedCornerShape(InkRadius.large),
        border = BorderStroke(InkBorder.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = InkSpacing.xs, vertical = InkSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
        ) {
            Icon(painterResource(icon), null, Modifier.size(InkHeaderTokens.iconSize))
            Text(
                value,
                Modifier.fillMaxWidth(),
                style = InkTextStyles.subPageTitle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(label),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
