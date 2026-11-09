package dev.twotechy.inknest.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun DateTimeHeader(tick: Long, stats: HomeReadingStats?, showStats: Boolean = true) {
    val context = LocalContext.current
    val locale = currentUiLocale()
    val time = remember(tick, locale) {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a",
            locale,
        ).format(Date(tick))
    }
    val date = remember(tick, locale) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"),
            locale,
        ).format(Date(tick))
    }
    val captions =
        listOf(stringResource(R.string.home_today), stringResource(R.string.stats_streak))
    val values =
        listOf(
            formatReadingTime(stats?.todayMinutes),
            stats?.streakDays?.let { stringResource(R.string.stats_days_value, it) }
                ?: stringResource(R.string.unknown_value),
        )
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val clockWidth = measurer.measure(time, InkTextStyles.displayClock, softWrap = false)
                .size.width
            val statsWidth = captions.indices.sumOf { index ->
                maxOf(
                    measurer.measure(captions[index], InkTextStyles.bodySmall, softWrap = false)
                        .size.width,
                    measurer.measure(values[index], InkTextStyles.cardTitle, softWrap = false)
                        .size.width,
                )
            }
            val fitsBesideClock = with(density) {
                (clockWidth + statsWidth).toDp() + InkSpacing.xxl + InkSpacing.lg <= maxWidth
            }
            if (showStats && fitsBesideClock) {
                Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(InkSpacing.xxl),
                    ) {
                        Text(
                            time,
                            style = InkTextStyles.displayClock,
                            modifier = Modifier.weight(1f),
                        )
                        ReadingStats(captions, values)
                    }
                    DateLabel(date)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
                    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
                        Text(time, style = InkTextStyles.displayClock)
                        DateLabel(date)
                    }
                    if (showStats) ReadingStats(captions, values, Modifier.fillMaxWidth(), true)
                }
            }
        }
        HorizontalDivider(thickness = InkBorder.default, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun DateLabel(date: String) {
    Text(
        date,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReadingStats(
    captions: List<String>,
    values: List<String>,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
        captions.indices.forEach { index ->
            Column(
                if (expanded) Modifier.weight(1f) else Modifier,
                verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
            ) {
                Text(values[index], style = InkTextStyles.cardTitle)
                Text(
                    captions[index],
                    style = InkTextStyles.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun formatReadingTime(minutes: Int?): String = when {
    minutes == null || minutes < 0 -> stringResource(R.string.unknown_value)
    minutes < 60 -> stringResource(R.string.home_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.home_hours, minutes / 60)
    else -> stringResource(R.string.home_hours_minutes, minutes / 60, minutes % 60)
}
