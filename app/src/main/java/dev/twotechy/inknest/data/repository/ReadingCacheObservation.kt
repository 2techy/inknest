package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.reading.CachedReading
import dev.twotechy.inknest.data.local.reading.ReadingBookEntity
import dev.twotechy.inknest.data.local.reading.ReadingCacheKey
import dev.twotechy.inknest.data.local.reading.ReadingSourceEntity
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus

// Match the observed partition to active config before publishing any cached row or sync metadata.
internal data class ReadingCacheObservation(
    val key: ReadingCacheKey?,
    val cached: CachedReading? = null,
) {
    fun readingState(active: ReadingCacheKey?, writeError: ReadingSyncIssue?): ReadingState = when {
        active == null -> ReadingState.NotConfigured
        key != active -> ReadingState.Loading
        cached != null -> cached.readingState()
        writeError != null -> ReadingState.Error(writeError)
        else -> ReadingState.Loading
    }

    fun syncStatus(
        active: ReadingCacheKey?,
        running: Boolean,
        writeError: ReadingSyncIssue?,
    ): ReadingSyncStatus {
        val source = cached?.source?.takeIf { key == active }
        val issue =
            writeError
                ?: source?.error?.let { value ->
                    ReadingSyncIssue.entries.find { it.name == value }
                }
        return ReadingSyncStatus(
            lastAttemptAt = source?.lastAttemptAt,
            lastSuccessAt = source?.lastSuccessAt,
            running = running,
            error = issue,
            stale = source.isStale(issue),
        )
    }
}

private fun CachedReading.readingState(): ReadingState {
    book?.let { return ReadingState.Content(it.toReadingItem()) }
    if (source.lastSuccessAt != null) return ReadingState.Empty
    return source.error?.let { ReadingState.Error(ReadingSyncIssue.valueOf(it)) }
        ?: ReadingState.Loading
}

private fun ReadingBookEntity.toReadingItem() = ReadingItem(
    id = ReadingItemId(sourceId, path),
    title = title,
    author = null,
    progress = null,
    availability = BookAvailability.valueOf(availability),
)

private fun ReadingSourceEntity?.isStale(issue: ReadingSyncIssue?): Boolean {
    val success = this?.lastSuccessAt ?: return false
    return issue != null || (lastAttemptAt ?: 0) > success
}
