package dev.twotechy.inknest.ui.library.details

import dev.twotechy.inknest.model.BookDetails
import java.time.ZoneId

internal data class BookDetailsUiState(
    val details: BookDetails = BookDetails(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val descriptionExpanded: Boolean = false,
    val historyExpanded: Boolean = false,
    val fileExpanded: Boolean = false,
)
