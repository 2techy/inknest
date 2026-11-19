package dev.twotechy.inknest.data.repository.reading

import dev.twotechy.inknest.model.PreparedReadingOpen
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenPreparation

internal interface ReadingOpenRepository {
    suspend fun prepare(item: ReadingItemId): ReadingOpenPreparation
    suspend fun isCurrent(prepared: PreparedReadingOpen): Boolean
}
