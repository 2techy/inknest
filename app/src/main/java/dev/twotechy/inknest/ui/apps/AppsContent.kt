package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.SectionHeading

@Composable
internal fun AppsContent(
    state: AppsUiState,
    columns: Int,
    icons: AppIconLoader?,
    onAppClick: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onMovePin: (String, Int) -> Unit,
) {
    val showPins = !state.isSearchActive && state.pinnedApps.isNotEmpty()
    val pinnedTitle = stringResource(R.string.pinned_apps)
    val allTitle = stringResource(R.string.all_apps)
    val content: @Composable (AppItemUiModel) -> Unit = { app ->
        AppItem(
            app,
            state.viewMode,
            icons,
            { onAppClick(app.componentId) },
            { onTogglePin(app.componentId, app.isPinned) },
            { offset -> onMovePin(app.componentId, offset) },
        )
    }
    if (state.viewMode == AppsViewMode.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showPins) {
                item(span = { GridItemSpan(columns) }) { SectionHeading(pinnedTitle) }
                items(state.pinnedApps, key = { "pinned_${it.componentId}" }) { content(it) }
                item(span = { GridItemSpan(columns) }) { SectionHeading(allTitle) }
            }
            items(state.allApps, key = { "all_${it.componentId}" }) { content(it) }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showPins) {
                item { SectionHeading(pinnedTitle) }
                items(state.pinnedApps, key = { "pinned_${it.componentId}" }) { content(it) }
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeading(allTitle)
                }
            }
            items(state.allApps, key = { "all_${it.componentId}" }) { content(it) }
        }
    }
}
