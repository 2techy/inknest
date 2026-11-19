package dev.twotechy.inknest.benchmarkfixture

internal object BenchmarkFixture {
    const val BOOKS = 1000
    const val HISTORY = 200
    const val EVENTS = 50_000
    const val VERSION = "2:$BOOKS:$HISTORY:$EVENTS:2026-11-01"
    const val CURRENT_TITLE = "Benchmark Book 0001"
    const val REFRESH_TRACE = "InkNest.foregroundRefresh"
    const val INDEX_TRACE = "InkNest.library.index"
    const val TIMEOUT_MILLIS = 600_000L
}
