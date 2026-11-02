package dev.twotechy.inknest.ui

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.twotechy.inknest.BuildConfig
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.home.BookPreview
import dev.twotechy.inknest.ui.home.HomeUiState
import dev.twotechy.inknest.ui.home.HomeReadingStats
import dev.twotechy.inknest.ui.home.homeStateFor
import java.text.SimpleDateFormat
import java.util.Date

private enum class Tab { Library, Stats, Home, Apps, Settings }

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
fun InkNestApp(clockTick: Long) {
    var selected by rememberSaveable { mutableStateOf(Tab.Home.name) }
    var previewMode by rememberSaveable { mutableStateOf("Content") }
    val tab = Tab.valueOf(selected)
    val state = remember(previewMode) { homeStateFor(previewMode) }
    val context = LocalContext.current
    val demoAction = {
        Toast.makeText(context, "Sample data only. Reading integration is coming later.", Toast.LENGTH_SHORT).show()
    }
    BackHandler(enabled = tab != Tab.Home) { selected = Tab.Home.name }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Use the app window, not a device model, resolution or physical diagonal.
        val useRail = maxWidth >= 600.dp
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { if (!useRail) InkNestBottomBar(tab) { selected = it.name } },
        ) { insets ->
            Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                if (useRail) InkNestNavigationRail(tab) { selected = it.name }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    BoxWithConstraints(Modifier.widthIn(max = 800.dp).fillMaxSize()) {
                        val gutter = if (maxWidth < 360.dp) 12.dp else if (maxWidth < 600.dp) 16.dp else 32.dp
                        val padding = PaddingValues(horizontal = gutter, vertical = 20.dp)
                        when (tab) {
                            Tab.Home -> HomeScreen(clockTick, state, Modifier.fillMaxSize(), padding, demoAction) {
                                selected = Tab.Settings.name
                            }
                            Tab.Settings -> SettingsScreen(previewMode, Modifier.fillMaxSize(), padding) { previewMode = it }
                            else -> PlaceholderScreen(tab, Modifier.fillMaxSize(), padding)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InkNestBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.onSurface
    Column(Modifier.background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.navigationBars)) {
        BoxWithConstraints(Modifier.fillMaxWidth().drawWithContent {
            drawContent()
            // Draw on the separator itself, centered over the selected destination.
            drawRect(lineColor, size = Size(size.width, 1.25.dp.toPx()))
            val tabWidth = size.width / Tab.entries.size
            val markerWidth = minOf(32.dp.toPx(), tabWidth * 0.7f)
            drawRect(selectedColor,
                topLeft = Offset(tabWidth * (selected.ordinal + 0.5f) - markerWidth / 2, 0f),
                size = Size(markerWidth, 3.dp.toPx()))
        }) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val labelWidth = with(density) { (maxWidth / 5 - 8.dp).toPx() }
            val labelsFit = Tab.entries.all {
                measurer.measure(it.name, NavigationLabelStyle.copy(fontWeight = FontWeight.SemiBold), softWrap = false).size.width <= labelWidth
            }
            Column {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    Tab.entries.forEach { tab ->
                        NavigationEntry(tab, selected, Modifier.weight(1f).fillMaxHeight(), onSelect, labelsFit)
                    }
                }
                if (!labelsFit) Text(selected.name, Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun InkNestNavigationRail(selected: Tab, onSelect: (Tab) -> Unit) {
    val railWidth = (80.dp * LocalDensity.current.fontScale).coerceIn(104.dp, 160.dp)
    Row(Modifier.fillMaxHeight()) {
        // Scroll rather than lose destinations in short landscape windows or at large font sizes.
        LazyColumn(Modifier.width(railWidth).fillMaxHeight(), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(Tab.entries) { tab -> NavigationEntry(tab, selected, Modifier.fillMaxWidth(), onSelect) }
        }
        VerticalDivider(thickness = 1.25.dp)
    }
}

@Composable
private fun NavigationEntry(tab: Tab, selected: Tab, modifier: Modifier, onSelect: (Tab) -> Unit, showLabel: Boolean = true) {
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
        TabIcon(tab, tab == selected)
        if (showLabel) Text(tab.name, textAlign = TextAlign.Center, style = NavigationLabelStyle,
            fontWeight = if (tab == selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun TabIcon(tab: Tab, selected: Boolean) {
    val drawable = when (tab) {
        Tab.Library -> if (selected) R.drawable.ic_nav_library_filled else R.drawable.ic_nav_library_outline
        Tab.Stats -> if (selected) R.drawable.ic_nav_stats_filled else R.drawable.ic_nav_stats_outline
        Tab.Home -> if (selected) R.drawable.ic_nav_home_filled else R.drawable.ic_nav_home_outline
        Tab.Apps -> if (selected) R.drawable.ic_nav_apps_filled else R.drawable.ic_nav_apps_outline
        Tab.Settings -> if (selected) R.drawable.ic_settings_filled else R.drawable.ic_settings_outline
    }
    Icon(
        painterResource(drawable),
        contentDescription = null,
        modifier = Modifier.size(NavigationIconSize),
        tint = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun HomeScreen(
    clockTick: Long,
    state: HomeUiState,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onDemoAction: () -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { DateTimeHeader(clockTick, (state as? HomeUiState.Content)?.stats) }
        when (state) {
            HomeUiState.Loading -> item { StatePanel("Gathering your books", "Loading your reading space…") }
            HomeUiState.Empty -> item {
                StatePanel("A little space for reading", "Connect KOReader to bring your books here.", "Open settings", onSettings)
            }
            is HomeUiState.Error -> item {
                StatePanel("Unable to load books", state.message, "Open settings", onSettings)
            }
            is HomeUiState.Content -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionHeading("Continue reading")
                        ContinueReadingCard(state.current, onDemoAction)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeading("Pinned apps")
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.pinnedApps.forEach { name ->
                                Column(
                                    Modifier.weight(1f).widthIn(min = 76.dp).quietClick(onDemoAction).padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(Modifier.size(48.dp).border(1.25.dp, MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                        Text(name.take(1), fontFamily = FontFamily.Serif, fontSize = 24.sp)
                                    }
                                    Text(name, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateTimeHeader(tick: Long, stats: HomeReadingStats?) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(tick, locale) {
        SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale).format(Date(tick))
    }
    val date = remember(tick, locale) { SimpleDateFormat("EEEE, MMMM d", locale).format(Date(tick)) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("InkNest", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
            if (BuildConfig.DEBUG) Text("Sample data", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val clockStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 48.sp, lineHeight = 56.sp)
            val clockWidth = measurer.measure(time, clockStyle, softWrap = false).size.width
            val statValues = listOf(formatReadingTime(stats?.todayMinutes), formatReadingTime(stats?.weekMinutes))
            val valueStyle = MaterialTheme.typography.titleMedium
            val captionStyle = MaterialTheme.typography.labelSmall
            val statsWidth = maxOf(
                statValues.maxOf { measurer.measure(it, valueStyle, softWrap = false).size.width },
                listOf("Today", "This week").maxOf { measurer.measure(it, captionStyle, softWrap = false).size.width },
            )
            val usableWidth = maxWidth - 20.dp
            val fitsBesideClock = with(density) {
                clockWidth.toDp() <= usableWidth * 0.58f &&
                    statsWidth.toDp() + 24.dp <= usableWidth * 0.42f
            }
            if (fitsBesideClock) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(time, style = clockStyle)
                        Text(date, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(0.42f)
                        .border(1.25.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Today", statValues[0])
                        MiniStat("This week", statValues[1])
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(time, style = clockStyle)
                    Text(date, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniStat("Today", statValues[0], Modifier.weight(1f).widthIn(min = 100.dp))
                        MiniStat("This week", statValues[1], Modifier.weight(1f).widthIn(min = 100.dp))
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp), thickness = 1.25.dp)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatReadingTime(minutes: Int?): String = when {
    minutes == null || minutes < 0 -> "—"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Composable
private fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ContinueReadingCard(book: BookPreview, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fontScale = LocalDensity.current.fontScale
                val coverWidth = (maxWidth * 0.23f).coerceIn(64.dp, 112.dp)
                // Reserve readable text width, including the user's accessibility scale.
                val stacked = maxWidth < coverWidth + 16.dp + 150.dp * fontScale
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BookCover(book, Modifier.width(coverWidth).aspectRatio(0.7f))
                        CurrentBookDetails(book)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        BookCover(book, Modifier.width(coverWidth).aspectRatio(0.7f))
                        CurrentBookDetails(book, Modifier.weight(1f))
                    }
                }
            }
            book.progress?.let { ProgressTrack(it) }
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                .quietClick(onClick).padding(14.dp), contentAlignment = Alignment.Center) {
                Text("Continue reading  →", color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CurrentBookDetails(book: BookPreview, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(book.title, style = MaterialTheme.typography.titleLarge)
        Text(book.author ?: "Unknown author", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProgressLabel(book.progress)
    }
}

@Composable
private fun BookCover(book: BookPreview, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
        .border(1.25.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
        .padding(8.dp).semantics { contentDescription = if (book.coverLabel == null) "No cover" else "Sample book cover" },
        contentAlignment = Alignment.Center) {
        // Cover art is decorative: drawing it avoids scaled text overflowing tiny thumbnails.
        Canvas(Modifier.fillMaxSize()) {
            if (book.coverLabel != null) {
                drawLine(ink, Offset(size.width * 0.15f, 0f), Offset(size.width * 0.15f, size.height), 1.dp.toPx())
                drawCircle(ink, size.width * 0.22f, Offset(size.width * 0.6f, size.height * 0.4f), style = Stroke(1.dp.toPx()))
                drawLine(ink, Offset(size.width * 0.35f, size.height * 0.75f), Offset(size.width * 0.85f, size.height * 0.75f), 1.dp.toPx())
            } else {
                drawLine(ink, Offset(size.width * 0.3f, size.height * 0.5f), Offset(size.width * 0.7f, size.height * 0.5f), 1.dp.toPx())
            }
        }
    }
}

@Composable
private fun ProgressLabel(progress: Int?) {
    Text(progress?.let { "$it% read" } ?: "Progress unavailable", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ProgressTrack(progress: Int) {
    val ink = MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(4.dp).semantics { contentDescription = "$progress percent read" }) {
        drawRect(track)
        drawRect(ink, size = Size(size.width * progress.coerceIn(0, 100) / 100f, size.height))
    }
}

@Composable
private fun StatePanel(title: String, detail: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().border(1.25.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        .padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) Text(action, Modifier.quietClick(onAction).padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PlaceholderScreen(tab: Tab, modifier: Modifier, contentPadding: PaddingValues) {
    val (title, description) = when (tab) {
        Tab.Library -> "Your reading shelf" to "Books you read will find a home here. Library browsing is coming in a later milestone."
        Tab.Stats -> "A habit, one page at a time" to "Your reading activity will appear here once a reading source is connected."
        else -> "Your everyday essentials" to "An app drawer and pinned apps are coming in a later milestone."
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { Text(tab.name, style = MaterialTheme.typography.headlineLarge) }
        item { StatePanel(title, description) }
    }
}

@Composable
private fun SettingsScreen(mode: String, modifier: Modifier, contentPadding: PaddingValues, onMode: (String) -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge) }
        item { StatePanel("Made for quiet screens", "E-Ink appearance · high contrast, static transitions and a minute clock.") }
        if (BuildConfig.DEBUG) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Home preview", "DEBUG BUILD ONLY")
                Text("Choose a state, then return Home. All books and apps shown are sample data.",
                    style = MaterialTheme.typography.bodyMedium)
                listOf("Content", "Empty", "Loading", "Error").forEach { choice ->
                    Text(
                        (if (choice == mode) "●  " else "○  ") + choice,
                        Modifier.fillMaxWidth().selectable(
                            selected = mode == choice, role = Role.RadioButton,
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                            onClick = { onMode(choice) },
                        ).padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        item { Text("InkNest  ${BuildConfig.VERSION_NAME}\n2techy", style = MaterialTheme.typography.bodyMedium) }
    }
}

private fun Modifier.quietClick(onClick: () -> Unit): Modifier = clickable(
    interactionSource = null,
    indication = null,
    role = Role.Button,
    onClick = onClick,
)
