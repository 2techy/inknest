package dev.twotechy.inknest.data.source

internal data class BookMetadataProbeResult(
    val title: String?,
    val author: String?,
    val cover: Boolean,
    val description: String? = null,
)
