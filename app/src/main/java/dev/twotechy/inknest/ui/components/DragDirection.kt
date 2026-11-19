package dev.twotechy.inknest.ui.components

internal enum class DragDirection { Undecided, Horizontal, Vertical }

// One direction per contact; an ambiguous diagonal never becomes an accidental tab change.
