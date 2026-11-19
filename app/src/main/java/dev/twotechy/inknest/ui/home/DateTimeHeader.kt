package dev.twotechy.inknest.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
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
    val is24Hour = DateFormat.is24HourFormat(context)
    val time = remember(tick, locale, is24Hour) {
        buildAnnotatedString {
            append(SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", locale).format(Date(tick)))
            if (!is24Hour) {
                withStyle(InkTextStyles.bodyMedium.toSpanStyle()) {
                    append(" " + SimpleDateFormat("a", locale).format(Date(tick)))
                }
            }
        }
    }
    val date = remember(tick, locale) {
        SimpleDateFormat(
            DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"),
            locale,
        ).format(Date(tick))
    }
    val minutes = formatReadingTime(stats?.todayMinutes)
    val streak = stats?.streakDays?.let { stringResource(R.string.stats_days_value, it) }
        ?: stringResource(R.string.unknown_value)
    val today = stringResource(R.string.home_today)
    val streakLabel = stringResource(R.string.stats_streak)
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            fun width(text: String, style: TextStyle) =
                measurer.measure(text, style, softWrap = false).size.width
            val clockWidth = maxOf(
                measurer.measure(time, InkTextStyles.homeClock, softWrap = false).size.width,
                width(date, InkTextStyles.bodyMedium),
            )
            val statsWidth = maxOf(
                width(today, InkTextStyles.bodyMedium),
                width(minutes, InkTextStyles.cardTitle),
                width(streak, InkTextStyles.bodyMedium),
                width(streakLabel, InkTextStyles.bodySmall),
            )
            val fitsBesideClock = with(density) {
                (clockWidth + statsWidth).toDp() + InkSpacing.xxl + InkSpacing.xxl +
                    InkSpacing.xxl + InkBorder.thin <= maxWidth
            }
            if (showStats && fitsBesideClock) {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClockAndDate(time, date, Modifier.weight(1f))
                    VerticalDivider(
                        Modifier.fillMaxHeight(),
                        thickness = InkBorder.thin,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    ReadingStatistics(today, minutes, streak, streakLabel)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
                    ClockAndDate(time, date)
                    if (showStats) ReadingStatistics(today, minutes, streak, streakLabel)
                }
            }
        }
        HorizontalDivider(thickness = InkBorder.default, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ClockAndDate(time: AnnotatedString, date: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        Text(time, style = InkTextStyles.homeClock)
        DateLabel(date)
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
private fun ReadingStatistics(today: String, minutes: String, streak: String, streakLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        Text(today, style = InkTextStyles.bodyMedium)
        StatLine(R.drawable.ic_nav_library_outline, minutes, InkTextStyles.cardTitle)
        StatLine(R.drawable.ic_streak, streak, InkTextStyles.bodyMedium, streakLabel)
    }
}

@Composable
private fun StatLine(icon: Int, value: String, style: TextStyle, description: String? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), description, Modifier.size(InkSpacing.xxl))
        Text(value, style = style)
    }
}

@Composable
private fun formatReadingTime(minutes: Int?): String = when {
    minutes == null || minutes < 0 -> stringResource(R.string.unknown_value)
    minutes < 60 -> stringResource(R.string.home_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.home_hours, minutes / 60)
    else -> stringResource(R.string.home_hours_minutes, minutes / 60, minutes % 60)
}
