package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.StatisticsBucket
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkStatsTokens
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Composable
internal fun StatsChart(buckets: List<StatisticsBucket>, options: StatsOptions) {
    val values = buckets.map { it.amount.value(options.unit) }
    val step = if (options.unit == StatisticsUnit.Time) 120L else 2L
    val peak = values.filterNotNull().maxOrNull() ?: 0
    val scale = (ceil(peak.toDouble() / step).toLong() * step).coerceAtLeast(step)
    val total = buckets.fold(StatisticsAmount()) { sum, day -> sum + day.amount }
    val description = buckets.map {
        "${it.date}: ${formatStatsAmount(it.amount, options.unit)}"
    }.joinToString("; ")
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        Row {
            ChartAxis(scale, options.unit)
            ChartBars(values, scale, description, Modifier.weight(1f))
        }
        ChartDates(buckets, options.granularity)
        Text(
            formatStatsAmount(total, options.unit),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ChartAxis(scale: Long, unit: StatisticsUnit) {
    val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 0 }
    val display = if (unit == StatisticsUnit.Time) scale / 60 else scale
    Column(
        Modifier.width(InkStatsTokens.chartAxisWidth).height(InkStatsTokens.chartHeight),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(
                if (unit ==
                    StatisticsUnit.Time
                ) {
                    R.string.stats_axis_minutes
                } else {
                    R.string.stats_axis_pages
                },
                number.format(display),
            ),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(number.format(display / 2), style = MaterialTheme.typography.labelSmall)
        Text(number.format(0), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChartBars(values: List<Long?>, scale: Long, description: String, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier.height(InkStatsTokens.chartHeight).semantics {
            contentDescription = description
        },
    ) {
        repeat(3) { index ->
            val y = size.height * index / 2
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val step = size.width / values.size.coerceAtLeast(1)
        val width = minOf(step * .65f, InkStatsTokens.maxBarWidth.toPx())
        values.forEachIndexed { index, value ->
            val x = index * step + (step - width) / 2
            if (value == null) {
                drawLine(
                    color,
                    Offset(x, size.height - 4.dp.toPx()),
                    Offset(
                        x + width,
                        size.height - 4.dp.toPx(),
                    ),
                    1.dp.toPx(),
                )
            } else if (value > 0) {
                val height = size.height * (value.toDouble() / scale).toFloat()
                drawRect(
                    color,
                    Offset(x, size.height - height),
                    Size(width, height.coerceAtLeast(1.dp.toPx())),
                )
            }
        }
    }
}

@Composable
private fun ChartDates(buckets: List<StatisticsBucket>, granularity: StatisticsGranularity) {
    val formatter = DateTimeFormatter.ofPattern(
        when (granularity) {
            StatisticsGranularity.Days, StatisticsGranularity.Weeks -> "d MMM"
            StatisticsGranularity.Months -> "MMM yy"
            StatisticsGranularity.Years -> "yyyy"
        },
    )
    val indices = listOf(0, buckets.lastIndex / 2, buckets.lastIndex)
        .filter { it in buckets.indices }.distinct()
    Row {
        Spacer(Modifier.width(InkStatsTokens.chartAxisWidth))
        Row(
            Modifier.weight(1f),
            horizontalArrangement = if (indices.size ==
                1
            ) {
                Arrangement.Center
            } else {
                Arrangement.SpaceBetween
            },
        ) {
            indices.forEach { index ->
                Text(
                    buckets[index].date.format(formatter),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
