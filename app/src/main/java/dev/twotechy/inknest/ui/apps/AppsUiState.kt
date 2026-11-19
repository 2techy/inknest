package dev.twotechy.inknest.ui.apps

data class AppsUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val viewMode: AppsViewMode = AppsViewMode.Grid,
    val pinnedApps: List<AppItemUiModel> = emptyList(),
    val allApps: List<AppItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppsError? = null,
    val canDismissError: Boolean = false,
)
