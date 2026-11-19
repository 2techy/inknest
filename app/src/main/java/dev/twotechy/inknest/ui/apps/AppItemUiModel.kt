package dev.twotechy.inknest.ui.apps

data class AppItemUiModel(
    val componentId: String,
    val label: String,
    val isPinned: Boolean,
    val iconVersion: Long = 0,
    val canMoveEarlier: Boolean = false,
    val canMoveLater: Boolean = false,
)
