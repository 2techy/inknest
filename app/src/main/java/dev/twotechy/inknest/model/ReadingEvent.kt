package dev.twotechy.inknest.model

data class ReadingEvent(
    val startedAt: Long,
    val seconds: Long,
    val bookId: Long,
    val page: Long? = null,
    val totalPages: Long? = null,
)
