package dev.twotechy.inknest.model

data class ReadingSyncStatus(
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val running: Boolean = false,
    val error: ReadingSyncIssue? = null,
    val stale: Boolean = false,
    val skippedHistoryEntries: Int = 0,
)
