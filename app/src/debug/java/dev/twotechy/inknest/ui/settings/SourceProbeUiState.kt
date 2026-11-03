package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.data.repository.ReaderProbeResult
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.SourceAccessState

internal data class SourceProbeUiState(
    val config: ReadingSourceConfig = ReadingSourceConfig(),
    val koreader: SourceAccessState = SourceAccessState.NotSelected,
    val books: SourceAccessState = SourceAccessState.NotSelected,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val reader: ReaderProbeResult? = null,
    val probing: Boolean = false,
)
