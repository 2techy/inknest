package dev.twotechy.inknest.ui.home

internal data class HomeActions(
    val reading: () -> Unit,
    val refresh: () -> Unit,
    val launchApp: (String) -> Unit,
    val openApps: () -> Unit,
    val settings: () -> Unit,
)
