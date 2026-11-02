package dev.twotechy.inknest.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.twotechy.inknest.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun DateTimeHeader(tick: Long, stats: HomeReadingStats?) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(tick, locale) {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a",
            locale,
        ).format(Date(tick))
    }
    val date =
        remember(tick, locale) { SimpleDateFormat("EEEE, MMMM d", locale).format(Date(tick)) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("InkNest", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
            if (BuildConfig.DEBUG) {
                Text(
                    "Sample data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val clockStyle =
                TextStyle(fontFamily = FontFamily.Serif, fontSize = 48.sp, lineHeight = 56.sp)
            val clockWidth = measurer.measure(time, clockStyle, softWrap = false).size.width
            val statValues =
                listOf(
                    formatReadingTime(stats?.todayMinutes),
                    formatReadingTime(stats?.weekMinutes),
                )
            val valueStyle = MaterialTheme.typography.titleMedium
            val captionStyle = MaterialTheme.typography.labelSmall
            val statsWidth = maxOf(
                statValues.maxOf { measurer.measure(it, valueStyle, softWrap = false).size.width },
                listOf("Today", "This week").maxOf {
                    measurer.measure(it, captionStyle, softWrap = false).size.width
                },
            )
            val usableWidth = maxWidth - 20.dp
            val fitsBesideClock = with(density) {
                clockWidth.toDp() <= usableWidth * 0.58f &&
                    statsWidth.toDp() + 24.dp <= usableWidth * 0.42f
            }
            if (fitsBesideClock) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        Modifier.weight(0.58f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(time, style = clockStyle)
                        Text(
                            date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        Modifier.weight(0.42f)
                            .border(
                                1.25.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MiniStat("Today", statValues[0])
                        MiniStat("This week", statValues[1])
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(time, style = clockStyle)
                    Text(
                        date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MiniStat("Today", statValues[0], Modifier.weight(1f).widthIn(min = 100.dp))
                        MiniStat(
                            "This week",
                            statValues[1],
                            Modifier.weight(1f).widthIn(min = 100.dp),
                        )
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp), thickness = 1.25.dp)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatReadingTime(minutes: Int?): String = when {
    minutes == null || minutes < 0 -> "—"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
