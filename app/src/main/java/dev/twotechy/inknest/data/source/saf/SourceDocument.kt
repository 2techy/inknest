package dev.twotechy.inknest.data.source.saf

internal data class SourceDocument(
    val uri: String,
    val mime: String?,
    val size: Long?,
    val modified: Long?,
)
