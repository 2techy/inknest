package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.ChoiceTab
import dev.twotechy.inknest.ui.components.quietClick
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkStatsTokens

@Composable
internal fun StatsUnits(selected: StatisticsUnit, onUnit: (StatisticsUnit) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            InkSpacing.xs,
        ),
    ) {
        StatisticsUnit.entries.forEach { unit ->
            ChoiceTab(
                stringResource(
                    if (unit == StatisticsUnit.Time) {
                        R.string.stats_unit_time
                    } else {
                        R.string.stats_unit_pages
                    },
                ),
                selected == unit,
                { onUnit(unit) },
            )
        }
    }
}

@Composable
internal fun StatsGranularities(
    selected: StatisticsGranularity,
    onGranularity: (StatisticsGranularity) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        StatisticsGranularity.entries.forEach { granularity ->
            val active = selected == granularity
            Column(
                Modifier.width(IntrinsicSize.Max)
                    .heightIn(min = InkHeaderTokens.touchTarget).semantics {
                        this.selected = active
                    }
                    .quietClick { onGranularity(granularity) },
            ) {
                Text(
                    stringResource(
                        when (granularity) {
                            StatisticsGranularity.Days -> R.string.stats_granularity_days
                            StatisticsGranularity.Weeks -> R.string.stats_granularity_weeks
                            StatisticsGranularity.Months -> R.string.stats_granularity_months
                            StatisticsGranularity.Years -> R.string.stats_granularity_years
                        },
                    ),
                    Modifier.padding(horizontal = InkSpacing.md, vertical = InkSpacing.sm),
                    style = MaterialTheme.typography.labelMedium,
                )
                HorizontalDivider(
                    color = if (active) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    thickness = if (active) InkStatsTokens.rangeMarker else InkBorder.default,
                )
            }
        }
    }
}
