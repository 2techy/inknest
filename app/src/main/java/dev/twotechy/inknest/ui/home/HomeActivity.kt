package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.ReadingSyncStatus
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsData
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.ReadingHeatmap
import dev.twotechy.inknest.ui.components.ReadingSyncNotice
import dev.twotechy.inknest.ui.components.quietClick
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkTextStyles
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class HomeActivity(
    val days: Map<LocalDate, StatisticsAmount>,
    val lastDay: LocalDate,
    val sync: ReadingSyncStatus,
)

internal fun homeActivity(data: StatisticsData, today: LocalDate): HomeActivity? {
    if (data.updatedAt == null) return null
    val first = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(51)
    return HomeActivity(data.days.filterKeys { it in first..today }, today, data.sync)
}

@Composable
internal fun HomeActivitySection(activity: HomeActivity, onOpenStats: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap)) {
        Column(
            Modifier.fillMaxWidth().quietClick(onOpenStats),
            verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap),
        ) {
            Text(stringResource(R.string.home_heatmap_title), style = InkTextStyles.sectionTitle)
            ReadingHeatmap(activity.days, activity.lastDay, StatisticsUnit.Time)
        }
        if (activity.sync.stale && !activity.sync.running) ReadingSyncNotice(activity.sync)
    }
}
