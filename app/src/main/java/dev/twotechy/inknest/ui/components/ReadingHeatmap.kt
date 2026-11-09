package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.theme.InkHeatmapPalette
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkStatsTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class HeatmapLayout(
    val first: java.time.LocalDate,
    val weeks: Int,
    val columnStep: Dp,
    val labelWidth: Dp,
) {
    val cellSize get() = minOf(
        InkStatsTokens.heatmapCellSize,
        (columnStep - InkStatsTokens.heatmapHorizontalGap).coerceAtLeast(1.dp),
    )
    val rowStep get() = cellSize + InkStatsTokens.heatmapVerticalGap
}

@Composable
internal fun ReadingHeatmap(
    activity: Map<LocalDate, StatisticsAmount>,
    lastDay: LocalDate,
    unit: StatisticsUnit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labelWidth = heatmapLabelWidth()
        val available = (maxWidth - labelWidth).coerceAtLeast(InkStatsTokens.heatmapCellSize)
        val columnSize = InkStatsTokens.heatmapCellSize + InkStatsTokens.heatmapHorizontalGap
        val weeks = (available / columnSize).toInt().coerceIn(16, 52)
        val layout = HeatmapLayout(
            lastDay.with(
                java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY),
            )
                .minusWeeks(weeks.toLong() - 1),
            weeks,
            available / weeks,
            labelWidth,
        )
        Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.md)) {
            HeatmapGrid(activity, lastDay, unit, layout)
        }
    }
}

@Composable
private fun heatmapLabelWidth(): Dp {
    val locale = currentUiLocale()
    val formatter = DateTimeFormatter.ofPattern("EEE", locale)
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodySmall
    val density = LocalDensity.current
    val measured = listOf(0L, 3L, 6L).maxOf { day ->
        val text = java.time.LocalDate.of(2024, 1, 1).plusDays(day).format(formatter)
        with(density) { measurer.measure(text, style, softWrap = false).size.width.toDp() }
    }
    return maxOf(
        InkStatsTokens.heatmapLabelWidth * density.fontScale.coerceAtLeast(1f),
        measured + InkSpacing.xs,
    )
}

@Composable
private fun HeatmapGrid(
    activity: Map<LocalDate, StatisticsAmount>,
    lastDay: LocalDate,
    unit: StatisticsUnit,
    layout: HeatmapLayout,
) {
    val locale = currentUiLocale()
    val values = (0 until layout.weeks * 7).map { index ->
        val date = layout.first.plusDays(index.toLong())
        date to (activity[date] ?: StatisticsAmount()).value(unit)
    }
    val intensity =
        HeatmapIntensity(values.filter { it.first <= lastDay }.map { it.second })
    val description = values.filter { it.first <= lastDay }.map {
        "${it.first}: ${formatStatsAmount(activity[it.first] ?: StatisticsAmount(), unit)}"
    }.joinToString("; ")
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        Row {
            androidx.compose.foundation.layout.Spacer(Modifier.width(layout.labelWidth))
            HeatmapMonths(layout)
        }
        Row {
            Box(Modifier.width(layout.labelWidth).height(layout.rowStep * 7)) {
                val labelHeight = with(LocalDensity.current) {
                    MaterialTheme.typography.bodySmall.lineHeight.toDp()
                }
                repeat(3) { index ->
                    Text(
                        layout.first.plusDays(
                            index * 3L,
                        ).format(DateTimeFormatter.ofPattern("EEE", locale)),
                        Modifier.offset(
                            y = layout.rowStep * (index * 3) + (layout.rowStep - labelHeight) / 2,
                        ),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Canvas(
                Modifier.width(layout.columnStep * layout.weeks).height(layout.rowStep * 7)
                    .semantics { contentDescription = description },
            ) {
                val column = size.width / layout.weeks
                val row = size.height / 7
                val gapY = InkStatsTokens.heatmapVerticalGap.toPx()
                val side = layout.cellSize.toPx()
                values.forEachIndexed { index, (date, value) ->
                    if (date <= lastDay) {
                        val top = Offset(
                            index / 7 * column + (column - side) / 2,
                            index % 7 * row + gapY / 2,
                        )
                        drawHeatmapCell(top, side, intensity.bucket(value))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawHeatmapCell(top: Offset, side: Float, bucket: Int?) {
    val radius = CornerRadius(InkStatsTokens.heatmapCornerRadius.toPx())
    val cell = Size(side, side)
    val outline = InkHeatmapPalette.emptyOutline
    drawRoundRect(InkHeatmapPalette.fills[bucket ?: 0], top, cell, radius)
    if (bucket == 0 || bucket == null) {
        drawRoundRect(
            outline,
            top,
            cell,
            radius,
            style = Stroke(InkStatsTokens.heatmapEmptyStroke.toPx()),
        )
    }
    if (bucket == null) drawLine(outline, top, top + Offset(side, side), 1.dp.toPx())
}

private data class MonthCaption(val text: String, val x: Dp, val width: Dp)

@Composable
private fun HeatmapMonths(layout: HeatmapLayout) {
    val style = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val locale = currentUiLocale()
    val captions = monthCaptions(layout, locale) { text ->
        with(density) { measurer.measure(text, style, softWrap = false).size.width.toDp() }
    }
    Box(
        Modifier.width(layout.columnStep * layout.weeks)
            .height(with(density) { style.lineHeight.toDp() }),
    ) {
        captions.forEach { caption ->
            Text(
                caption.text,
                Modifier.offset(x = caption.x).width(caption.width),
                softWrap = false,
                style = style,
            )
        }
    }
}

private fun monthCaptions(
    layout: HeatmapLayout,
    locale: Locale,
    measure: (String) -> Dp,
): List<MonthCaption> {
    val formatter = DateTimeFormatter.ofPattern("MMM", locale)
    val weeks = (0 until layout.weeks).filter { index ->
        val day = layout.first.plusWeeks(index.toLong())
        index == 0 || day.month != day.minusWeeks(1).month
    }
    val width = layout.columnStep * layout.weeks
    return buildList {
        weeks.forEachIndexed { index, week ->
            val text = layout.first.plusWeeks(week.toLong()).format(formatter)
            val measured = measure(text)
            val next = weeks.getOrNull(index + 1)
            // Omit an initial partial-month caption when the next month would collide with it.
            if (layout.overlapsInitialCaption(week, next, measured)) {
                return@forEachIndexed
            }
            val x = (layout.columnStep * week).coerceAtMost((width - measured).coerceAtLeast(0.dp))
            val previous = lastOrNull()
            if (previous == null || x >= previous.x + previous.width + InkSpacing.xs) {
                add(MonthCaption(text, x, measured))
            }
        }
    }
}

private fun HeatmapLayout.overlapsInitialCaption(week: Int, next: Int?, width: Dp): Boolean {
    if (week != 0) return false
    if (next == null) return false
    return width + InkSpacing.xs > columnStep * next
}
