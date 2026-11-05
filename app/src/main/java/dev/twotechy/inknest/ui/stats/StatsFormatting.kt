package dev.twotechy.inknest.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import java.text.NumberFormat

@Composable
internal fun formatStatsAmount(amount: StatisticsAmount, unit: StatisticsUnit): String =
    when (unit) {
        StatisticsUnit.Time -> formatReadingTime(amount.seconds)

        StatisticsUnit.Pages -> amount.pages?.let {
            stringResource(R.string.stats_pages_value, NumberFormat.getIntegerInstance().format(it))
        } ?: stringResource(R.string.stats_unknown)
    }

@Composable
internal fun formatReadingTime(seconds: Long): String = when {
    seconds > 0 && seconds < 60 -> stringResource(R.string.stats_under_minute)
    seconds < 3600 -> stringResource(R.string.stats_minutes_value, seconds / 60)
    else -> stringResource(R.string.stats_duration, seconds / 3600, (seconds % 3600) / 60)
}

@Composable
internal fun formatStatsAverage(value: Double?, unit: StatisticsUnit): String {
    if (value == null) return stringResource(R.string.stats_unknown)
    val number = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
        .format(if (unit == StatisticsUnit.Time) value / 60 else value)
    return stringResource(
        if (unit ==
            StatisticsUnit.Time
        ) {
            R.string.stats_average_minutes
        } else {
            R.string.stats_pages_value
        },
        number,
    )
}
