package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.StatisticsBucket
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.components.formatStatsAmount
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkStatsPalette
import dev.twotechy.inknest.ui.theme.InkStatsTokens
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@Composable
internal fun StatsChart(buckets: List<StatisticsBucket>, today: LocalDate, options: StatsOptions) {
    val values = buckets.map { if (it.date > today) 0L else it.amount.value(options.unit) }
    val scale = chartScale(values.filterNotNull().maxOrNull() ?: 0, options.unit)
    val description = buckets.filter { it.date <= today }.map {
        "${it.date} – ${minOf(it.lastDay, today)}: ${formatStatsAmount(it.amount, options.unit)}"
    }.joinToString("; ")
    val total = buckets.fold(StatisticsAmount()) { sum, bucket -> sum + bucket.amount }
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val axisWidth = InkStatsTokens.chartAxisWidth * fontScale
            val bucketWidth = when (options.period) {
                StatisticsPeriod.ThisWeek -> InkStatsTokens.chartDayWidth
                StatisticsPeriod.ThisYear -> InkStatsTokens.chartMonthWidth
                else -> InkStatsTokens.chartRangeWidth
            }
            val plotWidth = maxOf(maxWidth - axisWidth, bucketWidth * fontScale * buckets.size)
            Row {
                ChartAxis(scale, Modifier.width(axisWidth))
                key(options.period) {
                    Column(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                        ChartBars(
                            values,
                            scale.top,
                            description,
                            Modifier.width(plotWidth),
                        )
                        ChartDates(buckets, options.period, Modifier.width(plotWidth))
                    }
                }
            }
        }
        Text(
            stringResource(R.string.stats_chart_total, formatStatsAmount(total, options.unit)),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private data class ChartScale(val top: Long, val divisor: Long, val label: Int)

private fun chartScale(peak: Long, unit: StatisticsUnit): ChartScale {
    val divisor = when {
        unit == StatisticsUnit.Pages -> 1L
        peak >= 7200 -> 3600L
        else -> 60L
    }
    val target = (peak.toDouble() / divisor / 4).coerceAtLeast(1.0)
    val magnitude = 10.0.pow(floor(log10(target)))
    val step = listOf(1, 2, 5, 10).first { it * magnitude >= target } * magnitude
    val label = when (divisor) {
        1L -> R.string.stats_axis_pages
        3600L -> R.string.stats_axis_hours
        else -> R.string.stats_axis_minutes
    }
    return ChartScale(ceil(step * 4 * divisor).toLong(), divisor, label)
}

@Composable
private fun ChartAxis(scale: ChartScale, modifier: Modifier) {
    val number = NumberFormat.getIntegerInstance(currentUiLocale())
    Column(
        modifier.height(
            InkStatsTokens.chartHeight * LocalDensity.current.fontScale.coerceAtLeast(1f),
        ),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(5) { index ->
            Text(
                stringResource(
                    scale.label,
                    number.format(scale.top / scale.divisor * (4 - index) / 4),
                ),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ChartBars(values: List<Long?>, scale: Long, description: String, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    val grid = InkStatsPalette.chartGrid
    val lineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.labelSmall.lineHeight.toPx()
    }
    Canvas(
        modifier.height(
            InkStatsTokens.chartHeight * LocalDensity.current.fontScale.coerceAtLeast(1f),
        ).semantics {
            contentDescription =
                description
        },
    ) {
        val inset = lineHeight / 2
        val plotHeight = (size.height - lineHeight).coerceAtLeast(1f)
        val baseline = inset + plotHeight
        repeat(5) { index ->
            val y = inset + plotHeight * index / 4
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        drawLine(grid, Offset(0f, inset), Offset(0f, baseline), 1.dp.toPx())
        val step = size.width / values.size.coerceAtLeast(1)
        val width = minOf(step * .65f, InkStatsTokens.maxBarWidth.toPx())
        values.forEachIndexed { index, value ->
            val x = index * step + (step - width) / 2
            if (value == null) {
                drawLine(
                    color,
                    Offset(x, baseline - 4.dp.toPx()),
                    Offset(x + width, baseline - 4.dp.toPx()),
                    1.dp.toPx(),
                )
            } else if (value > 0) {
                val height = plotHeight * (value.toDouble() / scale).toFloat()
                drawRect(
                    color,
                    Offset(x, baseline - height),
                    Size(width, height.coerceAtLeast(1.dp.toPx())),
                )
            }
        }
    }
}

@Composable
private fun ChartDates(
    buckets: List<StatisticsBucket>,
    period: StatisticsPeriod,
    modifier: Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern(
        when (period) {
            StatisticsPeriod.ThisWeek -> "EEE"
            StatisticsPeriod.ThisMonth -> "d"
            StatisticsPeriod.ThisYear -> "MMM"
            StatisticsPeriod.AllTime -> "yyyy"
        },
        currentUiLocale(),
    )
    Row(modifier) {
        buckets.forEach { bucket ->
            val label = if (period == StatisticsPeriod.ThisMonth && bucket.date != bucket.lastDay) {
                stringResource(
                    R.string.stats_date_range,
                    bucket.date.format(formatter),
                    bucket.lastDay.format(formatter),
                )
            } else {
                bucket.date.format(formatter)
            }
            Text(
                label,
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
