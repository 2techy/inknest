package dev.twotechy.inknest.ui.home

import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItemId

data class BookUiModel(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverUri: String? = null,
    val availability: BookAvailability = BookAvailability.Available,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
)
