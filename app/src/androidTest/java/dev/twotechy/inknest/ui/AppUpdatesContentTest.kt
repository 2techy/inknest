package dev.twotechy.inknest.ui

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.MainActivity
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.model.AppUpdateError
import dev.twotechy.inknest.model.AppUpdateState
import dev.twotechy.inknest.model.UpdateDownloadStage
import dev.twotechy.inknest.model.UpdateSnapshot
import dev.twotechy.inknest.ui.settings.AppUpdatesContent
import dev.twotechy.inknest.ui.settings.ReleaseNotesDocument
import dev.twotechy.inknest.ui.settings.ReleaseNotesParser
import dev.twotechy.inknest.ui.settings.UpdateSettingsActions
import dev.twotechy.inknest.ui.theme.InkNestTheme
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppUpdatesContentTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var activity: MainActivity
    private val state = mutableStateOf(AppUpdateState(loaded = true))
    private val checks = AtomicInteger()
    private val downloads = AtomicInteger()
    private val installs = AtomicInteger()
    private val cancellations = AtomicInteger()
    private val websites = AtomicInteger()
    private val viewport = mutableStateOf<Pair<Float, Float>?>(null)
    private val layoutDirection = mutableStateOf(LayoutDirection.Ltr)
    private val openedLink = AtomicReference<String>()
    private lateinit var notes: ReleaseNotesDocument

    @Before fun launch() {
        notes = runBlocking { ReleaseNotesParser().parse(RELEASE.notes, RELEASE.pageUrl) }
        runBlocking { (context.applicationContext as InkNestApplication).awaitContainer() }
        activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        instrumentation.runOnMainSync {
            activity.setContent {
                val target = viewport.value
                val density = target?.let {
                    Density(activity.resources.displayMetrics.widthPixels / it.first, it.second)
                } ?: LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides density,
                    LocalLayoutDirection provides layoutDirection.value,
                ) {
                    InkNestTheme {
                        key(target) {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                AppUpdatesContent(
                                    state.value,
                                    notes,
                                    UpdateSettingsActions(
                                        check = {
                                            checks.incrementAndGet()
                                            state.value = state.value.copy(checking = true)
                                        },
                                        download = { downloads.incrementAndGet() },
                                        install = { installs.incrementAndGet() },
                                        cancel = { cancellations.incrementAndGet() },
                                        releasePage = { websites.incrementAndGet() },
                                        openLink = openedLink::set,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        instrumentation.waitForIdleSync()
    }

    @After fun close() = instrumentation.runOnMainSync { activity.finish() }

    @Test fun oneActionChangesFromCheckToDownloadWithoutWebsiteFallback() {
        click(context.getString(R.string.check_for_updates))
        assertEquals(1, checks.get())
        awaitText(context.getString(R.string.updates_checking))
        change(
            state.value.copy(
                checking = false,
                snapshot = UpdateSnapshot(release = RELEASE, checkedAt = 1),
            ),
        )
        awaitText(RELEASE.version)
        awaitText(context.getString(R.string.updates_download))
        assertFalse(hasText(context.getString(R.string.check_for_updates)))
        assertFalse(hasText("CalVer"))
        assertFalse(hasText("development build"))
        assertFalse(hasText(context.getString(R.string.updates_current)))
        assertFalse(hasText(context.getString(R.string.updates_automatic)))
        click(context.getString(R.string.updates_download))
        assertEquals(1, downloads.get())
        assertEquals(0, websites.get())
        change(AppUpdateState(loaded = true, error = AppUpdateError.Check))
        awaitText(context.getString(R.string.updates_check_failed))
        assertFalse(hasText(context.getString(R.string.updates_current)))
        click(context.getString(R.string.check_for_updates))
        assertEquals(2, checks.get())
    }

    @Test fun downloadIsReachableBeforeNotesAndProgressIsNotDuplicated() {
        change(
            AppUpdateState(
                loaded = true,
                canCompareVersion = true,
                automaticAllowed = true,
                available = true,
                snapshot = UpdateSnapshot(release = RELEASE),
            ),
        )
        awaitText(context.getString(R.string.updates_download))
        assertFalse(hasText(context.getString(R.string.check_for_updates)))
        assertFalse(hasText("Highlights"))
        click(context.getString(R.string.updates_download))
        assertEquals(1, downloads.get())
        click(context.getString(R.string.updates_release_notes))
        awaitText("Highlights")
        awaitText("A clearer update with code and help.")
        assertFalse(hasText("##"))
        assertFalse(hasText("**"))
        clickInlineLink("help")
        assertEquals("https://example.com/help", openedLink.get())
        click(context.getString(R.string.updates_release_notes))
        change(state.value.copy(download = UpdateDownloadStage.Ready))
        awaitText(context.getString(R.string.updates_install))
        assertFalse(hasText(context.getString(R.string.updates_download)))
        assertFalse(hasText(context.getString(R.string.check_for_updates)))
        click(context.getString(R.string.updates_install))
        assertEquals(1, installs.get())
        change(state.value.copy(download = UpdateDownloadStage.Downloading, progress = 37))
        val progress = context.getString(R.string.updates_progress, 37)
        awaitText(progress)
        assertEquals(1, nodes().count { it.text?.toString() == progress })
        assertTrue(hasText(context.getString(R.string.cancel)))
        click(context.getString(R.string.cancel))
        assertEquals(1, cancellations.get())
        change(state.value.copy(download = UpdateDownloadStage.Verifying))
        awaitText(context.getString(R.string.updates_verifying))
        assertFalse(hasText(context.getString(R.string.cancel)))
    }

    @Test fun metadataAndPrimaryActionRemainUsableOnNarrowScreensAndLargeFonts() {
        change(
            AppUpdateState(
                loaded = true,
                canCompareVersion = true,
                available = true,
                snapshot = UpdateSnapshot(release = RELEASE),
            ),
        )
        listOf(320f to 1f, 320f to 2f, 720f to 1f, 720f to 2f).forEachIndexed { index, size ->
            instrumentation.runOnMainSync { viewport.value = size }
            instrumentation.waitForIdleSync()
            awaitText(context.getString(R.string.updates_current_label))
            val current = bounds(context.getString(R.string.updates_current_label))
            val latest = bounds(context.getString(R.string.updates_latest_label))
            assertFalse("Version fields overlap at $size", Rect.intersects(current, latest))
            instrumentation.runOnMainSync { layoutDirection.value = LayoutDirection.Rtl }
            instrumentation.waitForIdleSync()
            val rtlCurrent = bounds(context.getString(R.string.updates_current_label))
            val rtlLatest = bounds(context.getString(R.string.updates_latest_label))
            assertFalse(
                "RTL version fields overlap at $size",
                Rect.intersects(rtlCurrent, rtlLatest),
            )
            if (current.top == latest.top) {
                assertTrue("Columns must mirror in RTL at $size", rtlCurrent.left > rtlLatest.left)
            }
            instrumentation.runOnMainSync { layoutDirection.value = LayoutDirection.Ltr }
            instrumentation.waitForIdleSync()
            reveal(context.getString(R.string.updates_download))
            click(context.getString(R.string.updates_download))
            assertEquals(index + 1, downloads.get())
            assertFalse(hasText(context.getString(R.string.check_for_updates)))
        }
    }

    private fun bounds(text: String): Rect = Rect().also { bounds ->
        nodes().first { it.text?.toString()?.contains(text) == true }.getBoundsInScreen(bounds)
    }

    private fun reveal(text: String) {
        repeat(8) {
            if (hasText(text)) return
            val scroll = nodes().first { it.isScrollable }
            assertTrue(scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            instrumentation.waitForIdleSync()
        }
        awaitText(text)
    }

    private fun change(next: AppUpdateState) {
        instrumentation.runOnMainSync { state.value = next }
        instrumentation.waitForIdleSync()
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val result = mutableListOf<AccessibilityNodeInfo>()
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return emptyList()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            node.refresh()
            result.add(node)
            repeat(node.childCount) { node.getChild(it)?.let(queue::add) }
        }
        return result
    }

    private fun hasText(text: String): Boolean =
        nodes().any { it.text?.toString()?.contains(text) == true }

    private fun awaitText(text: String) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (!hasText(text) && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        assertTrue(text, hasText(text))
    }

    private fun click(text: String) {
        awaitText(text)
        val current = nodes()
        var node = current.firstOrNull { it.text?.toString() == text }
            ?: current.first { it.text?.toString()?.contains(text) == true }
        while (!node.isClickable) node = checkNotNull(node.parent)
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun clickInlineLink(label: String) {
        val text = nodes().first { it.text?.toString()?.contains(label) == true }.text as Spanned
        val start = text.toString().indexOf(label)
        val span = text.getSpans(start, start + label.length, ClickableSpan::class.java).single()
        span.onClick(activity.window.decorView)
        instrumentation.waitForIdleSync()
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (openedLink.get() == null &&
            SystemClock.uptimeMillis() < deadline
        ) {
            SystemClock.sleep(50)
        }
    }

    private companion object {
        val RELEASE = AppRelease(
            "2026.11.0",
            "## Highlights\n\nA **clearer** update with `code` and [help](https://example.com/help).",
            "2026-11-10T00:00:00Z",
            "apk",
            "sums",
            1024,
        )
    }
}
