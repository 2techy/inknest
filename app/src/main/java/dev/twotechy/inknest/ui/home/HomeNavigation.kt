package dev.twotechy.inknest.ui.home

internal data class HomeNavigation(
    val apps: () -> Unit,
    val stats: () -> Unit,
    val settings: () -> Unit,
    val searchLibrary: () -> Unit = {},
)
