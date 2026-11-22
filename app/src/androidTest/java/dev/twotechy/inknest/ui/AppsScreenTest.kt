package dev.twotechy.inknest.ui

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.MainActivity
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.apps.AppItemActions
import dev.twotechy.inknest.ui.apps.AppItemUiModel
import dev.twotechy.inknest.ui.apps.AppsActions
import dev.twotechy.inknest.ui.apps.AppsScreen
import dev.twotechy.inknest.ui.apps.AppsUiState
import dev.twotechy.inknest.ui.apps.AppsViewMode
import dev.twotechy.inknest.ui.theme.InkNestTheme
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppsScreenTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var activity: MainActivity

    @Before fun launch() {
        runBlocking { (context.applicationContext as InkNestApplication).awaitContainer() }
        activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        instrumentation.waitForIdleSync()
    }

    @After fun close() = instrumentation.runOnMainSync { activity.finish() }

    @Test fun appsShowEachAppOnceAndLongPressTargetsItsMenu() {
        val alpha = AppItemUiModel("test.alpha/Main", "Alpha app", true, canMoveLater = true)
        val beta = AppItemUiModel("test.beta/Main", "Beta app", false)
        val apps = listOf(alpha, beta)
        val state = mutableStateOf(AppsUiState(allApps = apps, isLoading = false))
        val launches = mutableListOf<String>()
        val appInfo = mutableListOf<String>()
        val pinChanges = mutableListOf<Pair<String, Boolean>>()
        val moves = mutableListOf<Pair<String, Int>>()
        val infoLabel = context.getString(R.string.app_info)
        val pinnedLabel = context.getString(R.string.pinned_apps)
        val otherLabel = context.getString(R.string.other_apps)
        for (mode in AppsViewMode.entries) {
            state.value =
                state.value.copy(viewMode = mode, pinnedApps = listOf(alpha), allApps = apps)
            show(320f, 2f) {
                AppsScreen(
                    state = state.value,
                    actions = AppsActions(
                        onSearchQueryChanged = { state.value = state.value.copy(searchQuery = it) },
                        onSearchActiveChanged = {},
                        onToggleViewMode = {},
                        item = AppItemActions(
                            launch = { launches.add(it) },
                            openInfo = { appInfo.add(it) },
                            togglePin = { id, pinned -> pinChanges.add(id to pinned) },
                            movePin = { id, offset -> moves.add(id to offset) },
                        ),
                        onRetry = {},
                        onDismissError = {},
                    ),
                )
            }
            awaitText(pinnedLabel, true)
            awaitText(otherLabel, true)
            assertEquals(1, nodes().count { it.text?.toString() == alpha.label })
            touchApp(alpha.label)
            assertEquals(alpha.componentId, launches.last())
            val launchCount = launches.size
            touchApp(alpha.label, hold = true)
            awaitText(infoLabel, true)
            assertEquals(launchCount, launches.size)
            assertTrue(!actionNode(context.getString(R.string.move_earlier)).isEnabled)
            click(context.getString(R.string.move_later))
            awaitText(infoLabel, false)
            assertEquals(alpha.componentId to 1, moves.last())
            touchApp(beta.label, hold = true)
            click(context.getString(R.string.pin_app))
            awaitText(infoLabel, false)
            assertEquals(beta.componentId to false, pinChanges.last())
            touchApp(beta.label, hold = true)
            click(infoLabel)
            awaitText(infoLabel, false)
            assertEquals(beta.componentId, appInfo.last())
            touchApp(alpha.label, hold = true)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitText(infoLabel, false)

            checkAppsSections(state, alpha, beta)
        }
    }

    private fun checkAppsSections(
        state: MutableState<AppsUiState>,
        alpha: AppItemUiModel,
        beta: AppItemUiModel,
    ) {
        val pinnedLabel = context.getString(R.string.pinned_apps)
        val otherLabel = context.getString(R.string.other_apps)
        instrumentation.runOnMainSync {
            val unpinned = alpha.copy(isPinned = false)
            state.value =
                state.value.copy(pinnedApps = emptyList(), allApps = listOf(unpinned, beta))
        }
        awaitText(context.getString(R.string.all_apps), true)
        awaitText(pinnedLabel, false)
        instrumentation.runOnMainSync {
            val allPinned = listOf(beta.copy(isPinned = true), alpha)
            state.value = state.value.copy(pinnedApps = allPinned, allApps = allPinned)
        }
        awaitText(pinnedLabel, true)
        awaitText(otherLabel, false)
        val labels = nodes().mapNotNull { it.text?.toString() }.filter {
            it == alpha.label ||
                it == beta.label
        }
        assertEquals(listOf(beta.label, alpha.label), labels)
    }

    private fun touchApp(label: String, hold: Boolean = false) {
        val bounds = bounds(actionNode(label))
        val downTime = SystemClock.uptimeMillis()
        fun send(action: Int) {
            val event = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                action,
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                0,
            )
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        send(MotionEvent.ACTION_DOWN)
        if (hold) SystemClock.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 150)
        send(MotionEvent.ACTION_UP)
        instrumentation.waitForIdleSync()
    }

    @Test fun appsClearSearchKeepsSearchOpenAndShowsPinnedMatches() {
        val app = AppItemUiModel("test.reader/Main", "Reader app", true)
        val query = mutableStateOf("no-match")
        val clear = context.getString(R.string.clear_search_query)
        val info = context.getString(R.string.app_info)
        show(320f, 2f) {
            AppsScreen(
                state = AppsUiState(
                    searchQuery = query.value,
                    isSearchActive = true,
                    viewMode = AppsViewMode.List,
                    allApps = if (query.value.isEmpty()) listOf(app) else emptyList(),
                    pinnedApps = listOf(app),
                    isLoading = false,
                ),
                actions = AppsActions(
                    onSearchQueryChanged = { query.value = it },
                    onSearchActiveChanged = {},
                    onToggleViewMode = {},
                    item = AppItemActions(
                        launch = {},
                        openInfo = {},
                        togglePin = { _, _ -> },
                        movePin = { _, _ -> },
                    ),
                    onRetry = {},
                    onDismissError = {},
                ),
            )
        }
        awaitText(context.getString(R.string.no_apps_match, query.value), true)
        click(clear)
        awaitText(clear, false)
        awaitText(context.getString(R.string.close_search), true)
        awaitText(app.label, true)
        assertEquals("", query.value)
        assertEquals(1, nodes().count { it.text?.toString() == app.label })
        touchApp(app.label, hold = true)
        awaitText(info, true)
    }

    private fun bounds(node: AccessibilityNodeInfo): Rect = Rect().also(node::getBoundsInScreen)

    private fun show(width: Float, font: Float, content: @Composable () -> Unit) {
        val composed = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val density = activity.resources.displayMetrics.widthPixels / width
            activity.setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density, font),
                ) {
                    InkNestTheme {
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
        val deadline = SystemClock.uptimeMillis() + 2_000
        var root = instrumentation.uiAutomation.rootInActiveWindow
        while (root == null && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50)
            root = instrumentation.uiAutomation.rootInActiveWindow
        }
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val result = mutableListOf<AccessibilityNodeInfo>()
        queue.add(checkNotNull(root))
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
