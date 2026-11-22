package dev.twotechy.inknest.ui.apps

internal data class AppItemActions(
    val launch: (String) -> Unit,
    val openInfo: (String) -> Unit,
    val togglePin: (String, Boolean) -> Unit,
    val movePin: (String, Int) -> Unit,
)
