package dev.twotechy.inknest.ui.components

internal data class RefreshAction(
    val enabled: Boolean,
    val running: Boolean,
    val refresh: () -> Unit,
) {
    fun canRefresh(): Boolean = enabled && !running
}
