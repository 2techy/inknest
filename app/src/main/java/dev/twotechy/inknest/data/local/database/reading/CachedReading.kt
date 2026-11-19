package dev.twotechy.inknest.data.local.database.reading

import androidx.room3.Embedded

internal data class CachedReading(
    @Embedded val source: ReadingSourceEntity,
    @Embedded(prefix = "book_") val book: ReadingBookEntity?,
)
