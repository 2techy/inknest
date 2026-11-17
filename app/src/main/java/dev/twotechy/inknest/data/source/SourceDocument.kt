package dev.twotechy.inknest.data.source

internal data class SourceDocument(
    val uri: String,
    val mime: String?,
    val size: Long?,
    val modified: Long?,
)
