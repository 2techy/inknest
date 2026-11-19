package dev.twotechy.inknest.platform.icons

internal data class AppIconRequest(
    val componentId: String,
    val version: Long,
    val locale: String,
    val densityDpi: Int,
)
