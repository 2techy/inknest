package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.PreparedReadingOpen
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenPreparation

internal class TestReadingOpenRepository : ReadingOpenRepository {
    var action: suspend (ReadingItemId) -> ReadingOpenPreparation = {
        ReadingOpenPreparation.Failed(OpenResult.Unavailable)
    }
    override suspend fun prepare(item: ReadingItemId) = action(item)
    override suspend fun isCurrent(prepared: PreparedReadingOpen) = true
}
