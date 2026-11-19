package dev.twotechy.inknest.ui.library.details

internal data class BookDetailsActions(
    val back: () -> Unit,
    val open: () -> Unit,
    val description: () -> Unit,
    val history: () -> Unit,
    val file: () -> Unit,
    val page: (Int) -> Unit,
    val retry: () -> Unit,
)
