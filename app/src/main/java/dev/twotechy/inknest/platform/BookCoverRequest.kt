package dev.twotechy.inknest.platform

internal data class BookCoverRequest(
    val uri: String,
    val size: BookCoverSize,
    val color: Boolean = true,
)
