package dev.twotechy.inknest.model

data class ReadingItem(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverUri: String? = null,
    val availability: BookAvailability = BookAvailability.Available,
    val status: ReadingStatus = ReadingStatus.Unknown,
    val lastReadAt: Long? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val description: String? = null,
)
