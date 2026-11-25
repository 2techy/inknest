package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.model.ReadingItemId
import java.time.LocalDate

internal data class StatsDayNavigation(
    val day: LocalDate,
    val back: () -> Unit,
    val openBook: (ReadingItemId) -> Unit,
)
