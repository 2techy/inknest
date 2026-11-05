package dev.twotechy.inknest.data.source.stats

import dev.twotechy.inknest.model.ReadingEvent

internal fun interface StatisticsEventReader {
    suspend fun forEachBatch(consume: suspend (List<ReadingEvent>) -> Unit)

    companion object {
        const val BATCH_SIZE = 500
    }
}

internal interface StatisticsEventSource {
    // Snapshot retries may invoke consume again; each invocation is an independent attempt.
    suspend fun <T> withEvents(tree: String, consume: suspend (StatisticsEventReader) -> T): T
}
