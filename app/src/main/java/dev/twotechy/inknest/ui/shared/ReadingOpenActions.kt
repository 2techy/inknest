package dev.twotechy.inknest.ui.shared

import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.PreparedReadingOpen
import dev.twotechy.inknest.model.ReadingItemId

internal data class ReadingOpenActions(
    val open: (ReadingItemId) -> Unit,
    val claim: (Long) -> PreparedReadingOpen?,
    val complete: (Long, OpenResult) -> Unit,
    val cancel: () -> Unit,
    val dismiss: () -> Unit,
)
