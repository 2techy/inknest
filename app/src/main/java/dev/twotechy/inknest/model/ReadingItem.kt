package dev.twotechy.inknest.model

data class ReadingItemId(val sourceId: String, val path: String)

data class ReadingItem(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverUri: String? = null,
)

data class ReadingStats(val todayMinutes: Int?, val weekMinutes: Int?)

data class PinnedApp(val componentId: String, val label: String, val iconVersion: Long = 0)
