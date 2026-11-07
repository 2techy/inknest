package dev.twotechy.inknest.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkNavigationTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

internal enum class TopLevelDestination { Library, Stats, Home, Apps, Settings }

// Icon standard: Google Material Symbols Outlined, weight 300, optical size 24.
// Keep weight constant; selection changes only FILL (0 -> 1), plus the bar marker.
// Use local vectors with theme tint, without an icon background or extra icon library.
private val NavigationIconSize = InkNavigationTokens.iconSize

private val NavigationLabelStyle = InkTextStyles.labelMedium

@Composable
internal fun InkNestBottomBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().drawWithContent {
                drawContent()
                // Draw on the separator itself, centered over the selected destination.
                drawRect(lineColor, size = Size(size.width, InkBorder.thin.toPx()))
                val tabWidth = size.width / TopLevelDestination.entries.size
                val markerWidth = minOf(InkNavigationTokens.markerWidth.toPx(), tabWidth * 0.7f)
                drawRect(
                    selectedColor,
                    topLeft = Offset(tabWidth * (selected.ordinal + 0.5f) - markerWidth / 2, 0f),
                    size = Size(markerWidth, InkNavigationTokens.markerHeight.toPx()),
                )
            },
        ) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val labelWidth = with(density) { (maxWidth / 5 - InkSpacing.sm).toPx() }
            val labelsFit = TopLevelDestination.entries.all {
                measurer.measure(
                    stringResource(it.label()),
                    NavigationLabelStyle.copy(fontWeight = FontWeight.SemiBold),
                    softWrap = false,
                ).size.width <= labelWidth
            }
            Column {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationEntry(
                            tab,
                            selected,
                            Modifier.weight(1f).fillMaxHeight(),
                            onSelect,
                            labelsFit,
                        )
                    }
                }
                if (!labelsFit) {
                    Text(
                        stringResource(selected.label()),
                        Modifier.fillMaxWidth().padding(bottom = InkSpacing.sm),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
internal fun InkNestNavigationRail(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val scaledWidth = InkNavigationTokens.railWidth * LocalDensity.current.fontScale
    val railWidth = scaledWidth.coerceIn(
        InkNavigationTokens.railMinWidth,
        InkNavigationTokens.railMaxWidth,
    )
    Row(Modifier.fillMaxHeight()) {
        // Scroll rather than lose destinations in short landscape windows or at large font sizes.
        LazyColumn(
            Modifier.width(railWidth).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = InkSpacing.md),
        ) {
            items(TopLevelDestination.entries) { tab ->
                NavigationEntry(tab, selected, Modifier.fillMaxWidth(), onSelect)
            }
        }
        VerticalDivider(thickness = InkBorder.thin)
    }
}

@Composable
private fun NavigationEntry(
    tab: TopLevelDestination,
    selected: TopLevelDestination,
    modifier: Modifier,
    onSelect: (TopLevelDestination) -> Unit,
    showLabel: Boolean = true,
) {
    val label = stringResource(tab.label())
    val minHeight = if (showLabel) {
        InkNavigationTokens.minHeight
    } else {
        InkNavigationTokens.compactMinHeight
    }
    Column(
        modifier.heightIn(min = minHeight)
            .semantics { if (!showLabel) contentDescription = label }
            .selectable(
                selected = tab == selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(tab) },
            )
            .padding(horizontal = InkSpacing.xs, vertical = InkSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkNavigationTokens.iconLabelGap),
    ) {
        DestinationIcon(tab, tab == selected)
        if (showLabel) {
            Text(
                label,
                textAlign = TextAlign.Center,
                style = NavigationLabelStyle,
                fontWeight = if (tab == selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DestinationIcon(tab: TopLevelDestination, selected: Boolean) {
    val drawable = when (tab) {
        TopLevelDestination.Library ->
            if (selected) R.drawable.ic_nav_library_filled else R.drawable.ic_nav_library_outline

        TopLevelDestination.Stats ->
            if (selected) R.drawable.ic_nav_stats_filled else R.drawable.ic_nav_stats_outline

        TopLevelDestination.Home ->
            if (selected) R.drawable.ic_nav_home_filled else R.drawable.ic_nav_home_outline

        TopLevelDestination.Apps ->
            if (selected) R.drawable.ic_nav_apps_filled else R.drawable.ic_nav_apps_outline

        TopLevelDestination.Settings ->
            if (selected) R.drawable.ic_settings_filled else R.drawable.ic_settings_outline
    }
    Icon(
        painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(NavigationIconSize),
        tint = MaterialTheme.colorScheme.onSurface,
    )
}

private fun TopLevelDestination.label(): Int = when (this) {
    TopLevelDestination.Library -> R.string.library_title
    TopLevelDestination.Stats -> R.string.nav_stats
    TopLevelDestination.Home -> R.string.nav_home
    TopLevelDestination.Apps -> R.string.apps_title
    TopLevelDestination.Settings -> R.string.settings_title
}
