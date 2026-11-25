package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.model.StatisticsDayDetail
import java.time.LocalDate

internal data class StatsDayUiState(
    val day: LocalDate,
    val detail: StatisticsDayDetail,
    val page: Int,
)
