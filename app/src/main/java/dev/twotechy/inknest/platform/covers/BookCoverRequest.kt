package dev.twotechy.inknest.platform.covers

internal data class BookCoverRequest(
    val uri: String,
    val size: BookCoverSize,
    val color: Boolean = true,
)
