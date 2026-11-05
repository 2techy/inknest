package dev.twotechy.inknest.ui.apps

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.SearchField

@Composable
internal fun AppsTopBar(
    state: AppsUiState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit,
    onToggleViewMode: () -> Unit,
) {
    if (state.isSearchActive) {
        SearchField(
            state.searchQuery,
            stringResource(R.string.search_apps_hint),
            searchFocusRequester,
            onSearchQueryChanged,
            onClose = { onSearchActiveChanged(false) },
        )
    } else {
        ScreenHeader(stringResource(R.string.apps_title)) {
            HeaderIconButton(R.drawable.ic_search, stringResource(R.string.search_apps)) {
                onSearchActiveChanged(true)
            }
            val grid = state.viewMode == AppsViewMode.Grid
            HeaderIconButton(
                if (grid) R.drawable.ic_view_list else R.drawable.ic_view_grid,
                stringResource(if (grid) R.string.switch_to_list else R.string.switch_to_grid),
                onToggleViewMode,
            )
        }
    }
}
