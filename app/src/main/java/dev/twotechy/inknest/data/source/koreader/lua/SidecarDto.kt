package dev.twotechy.inknest.data.source.koreader.lua

internal data class SidecarDto(
    val percent: Double?,
    val status: String?,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
)
