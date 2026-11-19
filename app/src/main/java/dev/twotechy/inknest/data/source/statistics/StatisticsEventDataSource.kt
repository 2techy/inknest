package dev.twotechy.inknest.data.source.statistics

internal interface StatisticsEventDataSource {
    suspend fun fingerprint(tree: String): SnapshotFingerprint? = null

    // Snapshot retries may invoke consume again; each invocation is an independent attempt.
    suspend fun <T> withEvents(tree: String, consume: suspend (StatisticsEventReader) -> T): T
}
