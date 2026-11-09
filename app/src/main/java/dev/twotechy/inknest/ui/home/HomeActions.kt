package dev.twotechy.inknest.ui.home

internal data class HomeActions(
    val reading: () -> Unit,
    val refresh: () -> Unit,
    val launchApp: (String) -> Unit,
    val openStats: () -> Unit,
    val openApps: () -> Unit,
    val settings: () -> Unit,
)

internal data class HomeNavigation(
    val apps: () -> Unit,
    val stats: () -> Unit,
    val settings: () -> Unit,
)
