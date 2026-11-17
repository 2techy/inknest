package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.IOException

internal data class CurrentReadingSnapshot(
    val current: ReadingItem?,
    val warning: ReadingSyncIssue? = null,
    val history: List<ReadingItem>? = null,
    val metadata: Map<String, BookMetadata> = emptyMap(),
    val skippedHistoryEntries: Int = 0,
)
internal class ReadingRefreshException(val issue: ReadingSyncIssue, cause: Throwable? = null) :
    IOException(issue.name, cause)
internal fun interface CurrentReadingSource {
    suspend fun read(config: ReadingSourceConfig): CurrentReadingSnapshot
}
