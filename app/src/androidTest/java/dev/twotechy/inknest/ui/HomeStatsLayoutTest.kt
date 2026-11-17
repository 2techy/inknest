package dev.twotechy.inknest.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.MainActivity
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.ReadingSyncStatus
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.ChoiceTabGroup
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuEntry
import dev.twotechy.inknest.ui.components.ContextMenuEntryRow
import dev.twotechy.inknest.ui.components.ContextMenuHeader
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.HeaderIconPlacement
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.SearchField
import dev.twotechy.inknest.ui.home.HomeActions
import dev.twotechy.inknest.ui.home.HomeActivity
import dev.twotechy.inknest.ui.home.HomeActivitySection
import dev.twotechy.inknest.ui.home.HomeHeader
import dev.twotechy.inknest.ui.home.HomePinUiModel
import dev.twotechy.inknest.ui.home.HomePinnedApps
import dev.twotechy.inknest.ui.home.HomeReadingStats
import dev.twotechy.inknest.ui.home.HomeScreen
import dev.twotechy.inknest.ui.home.HomeUiState
import dev.twotechy.inknest.ui.home.ReadingSectionState
import dev.twotechy.inknest.ui.stats.StatsHighlights
import dev.twotechy.inknest.ui.stats.StatsOverview
import dev.twotechy.inknest.ui.theme.InkNestTheme
import java.io.File
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeStatsLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var activity: MainActivity
    private val today = LocalDate.of(2026, 11, 9)
    private val pins = (1..20).map { HomePinUiModel("test/$it", "Pin $it") }
    private val reading =
        HomeActivity(mapOf(today to StatisticsAmount(120, null)), today, ReadingSyncStatus())

    @Before fun launch() {
        runBlocking { (context.applicationContext as InkNestApplication).awaitContainer() }
        activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        instrumentation.waitForIdleSync()
    }

    @After fun close() = instrumentation.runOnMainSync { activity.finish() }

    @Test fun responsivePinsKeepOrderAndOverflowReachable() {
        for ((width, font) in listOf(320f to 1f, 320f to 2f, 720f to 1f, 720f to 2f)) {
            val opened = AtomicInteger()
            val launched = AtomicInteger()
            show(width, font) {
                HomePinnedApps(pins, null, {
                    launched.set(it.substringAfter('/').toInt())
                }, { opened.incrementAndGet() })
            }
            val labels = nodes().mapNotNull { it.text?.toString() }.filter { it.startsWith("Pin ") }
            assertTrue(labels.isNotEmpty())
            assertTrue(labels.size < pins.size)
            assertEquals(pins.take(labels.size).map { it.label }, labels)
            click("Pin 1")
            assertEquals("launch at width=$width font=$font", 1, launched.get())
            click(context.getString(R.string.home_all_pins_count, pins.size))
            assertEquals("overflow at width=$width font=$font", 1, opened.get())
        }
    }

    @Test fun heatmapOpensSelectedStatsUnitAndVisibilityIsIndependent() {
        val opened = mutableStateOf(false)
        val overview =
            StatsOverview(
                null,
                null,
                StatisticsAmount(120, null),
                emptyMap(),
                reading.days,
                today,
                emptyList(),
            )
        show(320f, 2f) {
            if (opened.value) {
                StatsHighlights(overview, StatisticsUnit.Pages)
            } else {
                HomeActivitySection(reading) { opened.value = true }
            }
        }
        click(context.getString(R.string.home_heatmap_title))
        awaitText(context.getString(R.string.stats_today), true)
        awaitText(context.getString(R.string.stats_minutes_value, 2), false)

        show(320f, 1f) {
            HomeScreen(
                HomeUiState.Loaded(
                    ReadingSectionState.Empty,
                    emptyList(),
                    stats = HomeReadingStats(2, 7),
                    activity = reading,
                ),
                HomeActions({}, {}, {}, {}, {}, {}),
                null,
            )
        }
        awaitText(context.getString(R.string.home_heatmap_title), true)
        show(320f, 1f, AppPreferences(showHomeHeatmap = false)) {
            HomeScreen(
                HomeUiState.Loaded(
                    ReadingSectionState.Empty,
                    emptyList(),
                    stats = HomeReadingStats(2, 7),
                    activity = reading,
                ),
                HomeActions({}, {}, {}, {}, {}, {}),
                null,
            )
        }
        awaitText(context.getString(R.string.home_heatmap_title), false)
        awaitText(context.getString(R.string.stats_streak), true)
        awaitText(context.getString(R.string.stats_days_value, 7), true)
    }

    @Test fun sharedFiltersAndPopupKeepSelectionAndDisabledActions() {
        val selected = mutableStateOf(0)
        val menu = mutableStateOf(false)
        val submenu = mutableStateOf(false)
        val disabledClicks = AtomicInteger()
        show(320f, 2f) {
            ChoiceTabGroup(listOf("All", "Reading", "Finished", "Unknown"), selected.value) {
                selected.value = it
            }
            HeaderIconButton(
                R.drawable.ic_options,
                "Options",
                placement = HeaderIconPlacement.Centered,
            ) {
                menu.value =
                    true
            }
            if (menu.value) {
                ContextMenu(onDismiss = { menu.value = false }) {
                    if (submenu.value) {
                        ContextMenuHeader("Layout") { submenu.value = false }
                        ContextMenuEntryRow(ContextMenuEntry("Grid", selected = true)) {
                            menu.value = false
                        }
                    } else {
                        ContextMenuEntryRow(
                            ContextMenuEntry("Layout", trailingIcon = R.drawable.ic_chevron_right),
                        ) {
                            submenu.value = true
                        }
                        ContextMenuEntryRow(ContextMenuEntry("Refresh", enabled = false)) {
                            disabledClicks.incrementAndGet()
                        }
                    }
                }
            }
        }
        screenshot("shared-filters-large-font.png")
        click("Reading")
        assertEquals(1, selected.value)
        awaitSelected("Reading")
        click("Options")
        awaitText("Refresh", true)
        screenshot("shared-popup-large-font.png")
        assertTrue(!actionNode("Refresh").isEnabled)
        assertEquals(0, disabledClicks.get())
        click("Layout")
        awaitText("Grid", true)
        awaitSelected("Grid")
        click(context.getString(R.string.back))
        awaitText("Refresh", true)
        click("Layout")
        click("Grid")
        awaitText("Grid", false)
        assertEquals(1, selected.value)
    }

    @Test fun pageHeaderTargetsMatchHomeAcrossDirectionAndFontScale() {
        for (direction in LayoutDirection.entries) {
            for (layout in listOf(
                HeaderLayout(320f, 1f, direction),
                HeaderLayout(320f, 2f, direction),
                HeaderLayout(600f, 1f, direction),
            )) {
                checkHeaderTargets(layout)
            }
        }
    }

    private data class HeaderLayout(
        val width: Float,
        val font: Float,
        val direction: LayoutDirection,
    )

    private fun checkHeaderTargets(layout: HeaderLayout) {
        val (width, font, direction) = layout
        val search = context.getString(R.string.library_search)
        val settings = context.getString(R.string.settings_title)
        val clicked = AtomicInteger()
        show(width, font, direction = direction) {
            HomeHeader(HomeActions({}, {}, {}, {}, {}, { clicked.incrementAndGet() }))
        }
        screenshot("header-home-$width-$font-$direction.png")
        val homeSearch = bounds(actionNode(search))
        val homeEnd = bounds(actionNode(settings))
        click(settings)
        assertEquals(1, clicked.get())
        show(width, font, direction = direction) {
            ScreenHeader("Thư viện / Library") {
                HeaderIconButton(R.drawable.ic_search, search) {}
                HeaderIconButton(
                    R.drawable.ic_options,
                    "Options",
                    placement = HeaderIconPlacement.Centered,
                ) {
                    clicked.incrementAndGet()
                }
            }
        }
        screenshot("header-page-$width-$font-$direction.png")
        val pageSearch = bounds(actionNode(search))
        val pageEnd = bounds(actionNode("Options"))
        assertEquals(homeSearch.left, pageSearch.left)
        assertEquals(homeSearch.right, pageSearch.right)
        assertEquals(homeEnd.left, pageEnd.left)
        assertEquals(homeEnd.right, pageEnd.right)
        assertTrue(!Rect.intersects(pageSearch, pageEnd))
        click("Options")
        assertEquals(2, clicked.get())
        show(width, font, direction = direction) {
            ScreenHeader("Chi tiết sách / Book details", {
                clicked.incrementAndGet()
            }, true)
        }
        screenshot("header-back-$width-$font-$direction.png")
        val back = bounds(actionNode(context.getString(R.string.back)))
        val title = bounds(
            nodes().first {
                it.text?.toString()?.contains("Book details") ==
                    true
            },
        )
        assertEquals(homeEnd.width(), back.width())
        assertEquals(homeEnd.height(), back.height())
        val arrow = bounds(
            nodes().first {
                it.contentDescription?.toString() == context.getString(R.string.back)
            },
        )
        assertTrue(!Rect.intersects(arrow, title))
        click(context.getString(R.string.back))
        assertEquals(3, clicked.get())
        show(width, font, direction = direction) {
            val focus = remember { FocusRequester() }
            SearchField("", "Search", focus, {}, { clicked.incrementAndGet() })
        }
        val close = bounds(actionNode(context.getString(R.string.close_search)))
        assertEquals(homeEnd.left, close.left)
        assertEquals(homeEnd.right, close.right)
        click(context.getString(R.string.close_search))
        assertEquals(4, clicked.get())
    }

    private fun bounds(node: AccessibilityNodeInfo): Rect = Rect().also(node::getBoundsInScreen)

    private fun show(
        width: Float,
        font: Float,
        preferences: AppPreferences = AppPreferences(),
        direction: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        val composed = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val density = activity.resources.displayMetrics.widthPixels / width
            activity.setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density, font),
                    LocalLayoutDirection provides direction,
                ) {
                    InkNestTheme(preferences) {
                        key(width, font) {
                            Column(Modifier.fillMaxWidth()) { content() }
                        }
                        SideEffect { composed.countDown() }
                    }
                }
            }
        }
        assertTrue("fixture composed", composed.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(200, 5_000)
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        val root = checkNotNull(instrumentation.uiAutomation.rootInActiveWindow)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val result = mutableListOf<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            node.refresh()
            result.add(node)
            repeat(node.childCount) { index -> node.getChild(index)?.let(queue::add) }
        }
        return result
    }

    private fun awaitText(text: String, present: Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (hasText(text) != present && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50)
        }
        assertEquals(text, present, hasText(text))
    }

    private fun hasText(text: String) = nodes().any {
        it.text?.toString()?.contains(text) == true ||
            it.contentDescription?.toString()?.contains(text) == true
    }

    private fun awaitSelected(text: String) {
        fun selected(): Boolean = nodes().filter {
            it.text?.toString()?.contains(text) == true
        }.any {
            var node: AccessibilityNodeInfo? = it
            while (node != null) {
                if (node.isSelected ||
                    node.stateDescription?.toString() == "Selected"
                ) {
                    return@any true
                }
                node = node.parent
            }
            false
        }
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (!selected() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        assertTrue(
            nodes().joinToString {
                "${it.text}/${it.contentDescription}/${it.isSelected}/${it.stateDescription}"
            },
            selected(),
        )
    }

    private fun screenshot(name: String) {
        // Wait for the replacement content to reach the rendering surface, not only semantics.
        val frame = CountDownLatch(1)
        instrumentation.runOnMainSync {
            activity.window.decorView.postOnAnimation {
                activity.window.decorView.postOnAnimation { frame.countDown() }
            }
        }
        assertTrue("frame rendered", frame.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }

    private fun actionNode(text: String): AccessibilityNodeInfo {
        val candidates = nodes()
        var node = candidates.firstOrNull {
            it.text?.toString() == text || it.contentDescription?.toString() == text
        } ?: candidates.first {
            it.text?.toString()?.contains(text) == true ||
                it.contentDescription?.toString()?.contains(text) == true
        }
        while (!node.isClickable && node.isEnabled) node = checkNotNull(node.parent)
        return node
    }

    private fun click(text: String) {
        assertTrue(actionNode(text).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(100, 5_000)
    }
}
