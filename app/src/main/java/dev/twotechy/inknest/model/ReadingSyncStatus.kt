package dev.twotechy.inknest.model

enum class ReadingSyncIssue { PermissionLost, MissingData, InvalidData, Provider, Cache }
enum class BookAvailability { Available, Missing, PermissionLost, Unresolved }

data class ReadingSyncStatus(
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val running: Boolean = false,
    val error: ReadingSyncIssue? = null,
    val stale: Boolean = false,
)
