package dev.twotechy.inknest.ui

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.MainActivity
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.ReadingSyncStatus
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.home.HomeActions
import dev.twotechy.inknest.ui.home.HomeActivity
import dev.twotechy.inknest.ui.home.HomeActivitySection
import dev.twotechy.inknest.ui.home.HomePinUiModel
import dev.twotechy.inknest.ui.home.HomePinnedApps
import dev.twotechy.inknest.ui.home.HomeReadingStats
import dev.twotechy.inknest.ui.home.HomeScreen
import dev.twotechy.inknest.ui.home.HomeUiState
import dev.twotechy.inknest.ui.home.ReadingSectionState
import dev.twotechy.inknest.ui.stats.StatsHighlights
import dev.twotechy.inknest.ui.stats.StatsOverview
import dev.twotechy.inknest.ui.theme.InkNestTheme
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
        awaitText(context.getString(R.string.stats_pages_today), true)
        awaitText(context.getString(R.string.stats_min_today), false)
        val labelTops = listOf(
            R.string.stats_pages_today,
            R.string.stats_daily_average,
            R.string.stats_streak,
        ).map { label ->
            val bounds = Rect()
            nodes().first {
                it.text?.toString() == context.getString(label)
            }.getBoundsInScreen(bounds)
            bounds.top
        }
        assertEquals("highlights share one row", 1, labelTops.distinct().size)

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

    private fun show(
        width: Float,
        font: Float,
        preferences: AppPreferences = AppPreferences(),
        content: @Composable () -> Unit,
    ) {
        val composed = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val density = activity.resources.displayMetrics.widthPixels / width
            activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(density, font)) {
                    InkNestTheme(preferences) {
                        Column(Modifier.fillMaxWidth()) { content() }
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

    private fun hasText(text: String) = nodes().any { it.text?.toString()?.contains(text) == true }

    private fun click(text: String) {
        var node = nodes().first { it.text?.toString()?.contains(text) == true }
        while (!node.isClickable) node = checkNotNull(node.parent)
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(100, 5_000)
    }
}
