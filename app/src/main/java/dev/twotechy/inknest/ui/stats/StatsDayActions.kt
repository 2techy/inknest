package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.model.ReadingItemId

internal data class StatsDayActions(
    val page: (Int) -> Unit,
    val back: () -> Unit,
    val retry: () -> Unit,
    val openBook: (ReadingItemId) -> Unit,
)
