package dev.twotechy.inknest.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.icons.AppIconLoader
import dev.twotechy.inknest.ui.components.SectionHeading
import dev.twotechy.inknest.ui.theme.InkAppGridTokens
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AppsContent(state: AppsUiState, icons: AppIconLoader?, actions: AppItemActions) {
    val sections = remember(state.pinnedApps, state.allApps, state.isSearchActive) {
        appSections(state.allApps, state.pinnedApps, state.isSearchActive)
    }
    val item: @Composable (AppItemUiModel) -> Unit = { app ->
        AppItem(app, state.viewMode, icons, actions)
    }
    if (state.viewMode == AppsViewMode.Grid) {
        AppsGrid(sections, item)
    } else {
        AppsList(sections, item)
    }
}

private data class AppSection(val title: Int?, val apps: List<AppItemUiModel>)

private fun appSections(
    allApps: List<AppItemUiModel>,
    pinnedApps: List<AppItemUiModel>,
    isSearchActive: Boolean,
): List<AppSection> {
    if (isSearchActive) return listOf(AppSection(null, allApps))
    if (pinnedApps.isEmpty()) {
        return if (allApps.isEmpty()) {
            emptyList()
        } else {
            listOf(
                AppSection(R.string.all_apps, allApps),
            )
        }
    }
    val pinnedIds = pinnedApps.mapTo(mutableSetOf(), AppItemUiModel::componentId)
    val others = allApps.filterNot { it.componentId in pinnedIds }
    return buildList {
        add(AppSection(R.string.pinned_apps, pinnedApps))
        if (others.isNotEmpty()) add(AppSection(R.string.other_apps, others))
    }
}

@Composable
private fun AppsGrid(sections: List<AppSection>, appItem: @Composable (AppItemUiModel) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(InkAppGridTokens.columnsFor(maxWidth, fontScale)),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(InkAppGridTokens.horizontalGap),
            verticalArrangement = Arrangement.spacedBy(InkAppGridTokens.verticalGap),
        ) {
            sections.forEachIndexed { index, section ->
                section.title?.let { title ->
                    item(key = title, span = { GridItemSpan(maxLineSpan) }) {
                        AppsSectionHeading(title, divided = index > 0)
                    }
                }
                items(section.apps, key = AppItemUiModel::componentId) { appItem(it) }
            }
        }
    }
}

@Composable
private fun AppsList(sections: List<AppSection>, appItem: @Composable (AppItemUiModel) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        sections.forEachIndexed { sectionIndex, section ->
            section.title?.let { title ->
                item(key = title) {
                    AppsSectionHeading(
                        title,
                        divided = sectionIndex > 0,
                        dividerTopPadding = InkSpacing.sm,
                    )
                }
            }
            itemsIndexed(section.apps, key = { _, app -> app.componentId }) { index, app ->
                if (index > 0) AppDivider()
                appItem(app)
            }
        }
    }
}

@Composable
private fun AppsSectionHeading(title: Int, divided: Boolean, dividerTopPadding: Dp = 0.dp) {
    Column(
        Modifier.semantics { heading() },
        verticalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        if (divided) {
            AppDivider(Modifier.padding(top = dividerTopPadding))
        }
        SectionHeading(stringResource(title))
    }
}

@Composable
private fun AppDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, InkBorder.thin, MaterialTheme.colorScheme.outlineVariant)
}
