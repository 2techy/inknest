package dev.twotechy.inknest.data.source.statistics

import dev.twotechy.inknest.model.ReadingEvent

internal fun interface StatisticsEventReader {
    val fingerprint: SnapshotFingerprint? get() = null

    suspend fun forEachBatch(consume: suspend (List<ReadingEvent>) -> Unit)

    suspend fun forEachBookBatch(consume: suspend (List<StatisticsBookIdentity>) -> Unit) {}

    companion object {
        const val BATCH_SIZE = 500
    }
}
