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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.twotechy.inknest.R

internal enum class TopLevelDestination { Library, Stats, Home, Apps, Settings }

// Icon standard: Google Material Symbols Outlined, weight 300, optical size 24.
// Keep weight constant; selection changes only FILL (0 -> 1), plus the bar marker.
// Use local vectors with theme tint, without an icon background or extra icon library.
private val NavigationIconSize = 28.dp

private val NavigationLabelStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium,
)

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
                drawRect(lineColor, size = Size(size.width, 1.25.dp.toPx()))
                val tabWidth = size.width / TopLevelDestination.entries.size
                val markerWidth = minOf(32.dp.toPx(), tabWidth * 0.7f)
                drawRect(
                    selectedColor,
                    topLeft = Offset(tabWidth * (selected.ordinal + 0.5f) - markerWidth / 2, 0f),
                    size = Size(markerWidth, 3.dp.toPx()),
                )
            },
        ) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val labelWidth = with(density) { (maxWidth / 5 - 8.dp).toPx() }
            val labelsFit = TopLevelDestination.entries.all {
                measurer.measure(
                    it.name,
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
                        selected.name,
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
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
    val railWidth = (80.dp * LocalDensity.current.fontScale).coerceIn(104.dp, 160.dp)
    Row(Modifier.fillMaxHeight()) {
        // Scroll rather than lose destinations in short landscape windows or at large font sizes.
        LazyColumn(
            Modifier.width(railWidth).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(TopLevelDestination.entries) { tab ->
                NavigationEntry(tab, selected, Modifier.fillMaxWidth(), onSelect)
            }
        }
        VerticalDivider(thickness = 1.25.dp)
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
    Column(
        modifier.padding(vertical = 2.dp)
            .heightIn(min = if (showLabel) 64.dp else 48.dp)
            .semantics { if (!showLabel) contentDescription = tab.name }
            .selectable(
                selected = tab == selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(tab) },
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DestinationIcon(tab, tab == selected)
        if (showLabel) {
            Text(
                tab.name,
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
