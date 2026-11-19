package dev.twotechy.inknest.ui.components

import androidx.compose.ui.semantics.selected

internal data class ContextMenuEntry(
    val label: String,
    val enabled: Boolean = true,
    val leadingIcon: Int? = null,
    val trailingIcon: Int? = null,
    val selected: Boolean? = null,
)
