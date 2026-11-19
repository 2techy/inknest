package dev.twotechy.inknest.data.source.koreader

import dev.twotechy.inknest.data.source.books.BookMetadata
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncIssue

internal data class CurrentReadingSnapshot(
    val current: ReadingItem?,
    val warning: ReadingSyncIssue? = null,
    val history: List<ReadingItem>? = null,
    val metadata: Map<String, BookMetadata> = emptyMap(),
    val skippedHistoryEntries: Int = 0,
    val fingerprint: SourceMetadataFingerprint? = null,
)
