package dev.twotechy.inknest.ui.apps

internal data class AppsActions(
    val onSearchQueryChanged: (String) -> Unit,
    val onSearchActiveChanged: (Boolean) -> Unit,
    val onToggleViewMode: () -> Unit,
    val item: AppItemActions,
    val onRetry: () -> Unit,
    val onDismissError: () -> Unit,
)
