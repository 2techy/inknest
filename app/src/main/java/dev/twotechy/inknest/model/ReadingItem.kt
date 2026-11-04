package dev.twotechy.inknest.model

data class ReadingItemId(val sourceId: String, val path: String)

data class ReadingItem(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverUri: String? = null,
    val availability: BookAvailability = BookAvailability.Available,
    val status: ReadingStatus = ReadingStatus.Unknown,
    val lastReadAt: Long? = null,
)

data class ReadingStats(val todayMinutes: Int?, val weekMinutes: Int?)

data class PinnedApp(val componentId: String, val label: String, val iconVersion: Long = 0)

enum class ReadingStatus { Reading, Finished, Abandoned, Unknown }
