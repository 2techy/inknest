package dev.twotechy.inknest.ui

import android.content.Intent
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.MainActivity
import dev.twotechy.inknest.ui.components.AppScaffold
import dev.twotechy.inknest.ui.components.BlockContentGestures
import dev.twotechy.inknest.ui.components.LocalContentGestureSession
import dev.twotechy.inknest.ui.components.RefreshAction
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.theme.InkNestTheme
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ContentGesturesIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var activity: MainActivity
    private val selected = mutableStateOf(TopLevelDestination.Home)
    private val session = MutableStateFlow(0L)
    private val refreshes = AtomicInteger()
    private val clicks = AtomicInteger()
    private val list = LazyListState()
    private val searching = mutableStateOf(false)
    private val empty = mutableStateOf(false)
    private val blocked = mutableStateOf(false)
    private val showNavigation = mutableStateOf(true)
    private var density = 1f

    @Before fun showFixture() {
        val context = instrumentation.targetContext
        runBlocking { (context.applicationContext as InkNestApplication).awaitContainer() }
        activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            density = activity.resources.displayMetrics.density
            activity.setContent {
                InkNestTheme {
                    CompositionLocalProvider(
                        LocalContentGestureSession provides session,
                    ) {
                        AppScaffold(selected.value, {
                            selected.value = it
                        }, showNavigation.value) { padding ->
                            BlockContentGestures(blocked.value)
                            ScreenScaffold(
                                Modifier.fillMaxSize(),
                                padding,
                                header = { Text("Gesture probe") },
                                gesturesEnabled = !searching.value,
                                refresh = RefreshAction(!searching.value, false) {
                                    refreshes.incrementAndGet()
                                },
                            ) {
                                if (empty.value) {
                                    Text("Empty")
                                } else {
                                    LazyColumn(Modifier.fillMaxSize(), state = list) {
                                        items(60) { index ->
                                            Box(
                                                Modifier.fillParentMaxWidth().height(
                                                    64.dp,
                                                ).clickable {
                                                    clicks.incrementAndGet()
                                                },
                                            ) {
                                                Text("Item $index")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        instrumentation.waitForIdleSync()
    }

    @After fun close() {
        instrumentation.runOnMainSync { activity.finish() }
    }

    @Test fun horizontalSwipeWorksOnClickableItemsAndHonorsSearchAndCancel() {
        drag(Offset(300f, 250f), Offset(80f, 250f))
        assertEquals(TopLevelDestination.Apps, selected.value)
        assertEquals(0, clicks.get())
        drag(Offset(300f, 250f), Offset(80f, 250f), MotionEvent.ACTION_CANCEL)
        assertEquals(TopLevelDestination.Apps, selected.value)
        instrumentation.runOnMainSync { searching.value = true }
        instrumentation.waitForIdleSync()
        drag(Offset(300f, 250f), Offset(80f, 250f))
        assertEquals(TopLevelDestination.Apps, selected.value)
        assertEquals(0, refreshes.get())
        instrumentation.runOnMainSync {
            searching.value = false
            blocked.value = true
        }
        instrumentation.waitForIdleSync()
        drag(Offset(300f, 250f), Offset(80f, 250f))
        drag(Offset(200f, 200f), Offset(200f, 390f))
        assertEquals(TopLevelDestination.Apps, selected.value)
        assertEquals(0, refreshes.get())
        instrumentation.runOnMainSync {
            blocked.value = false
            showNavigation.value = false
        }
        instrumentation.waitForIdleSync()
        drag(Offset(300f, 250f), Offset(80f, 250f))
        assertEquals(TopLevelDestination.Apps, selected.value)
    }

    @Test fun pullRefreshHonorsTopShortContentCancelAndSessionReset() {
        drag(Offset(200f, 200f), Offset(200f, 390f))
        assertEquals(1, refreshes.get())
        drag(Offset(200f, 390f), Offset(200f, 180f))
        drag(Offset(200f, 200f), Offset(200f, 600f))
        assertEquals(1, refreshes.get())
        instrumentation.runOnMainSync { empty.value = true }
        instrumentation.waitForIdleSync()
        drag(Offset(200f, 200f), Offset(200f, 390f))
        assertEquals(2, refreshes.get())
        drag(Offset(200f, 200f), Offset(200f, 390f), MotionEvent.ACTION_CANCEL)
        assertEquals(2, refreshes.get())
        drag(Offset(200f, 200f), Offset(200f, 390f), beforeRelease = {
            instrumentation.runOnMainSync { session.value++ }
            instrumentation.waitForIdleSync()
        })
        assertEquals(2, refreshes.get())
    }

    private fun drag(
        start: Offset,
        end: Offset,
        release: Int = MotionEvent.ACTION_UP,
        beforeRelease: () -> Unit = {},
    ) {
        val down = SystemClock.uptimeMillis()
        fun send(action: Int, px: Float, py: Float) {
            val event = MotionEvent.obtain(
                down,
                SystemClock.uptimeMillis(),
                action,
                px * density,
                py * density,
                0,
            )
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        send(MotionEvent.ACTION_DOWN, start.x, start.y)
        repeat(12) { step ->
            SystemClock.sleep(16)
            val fraction = (step + 1) / 12f
            val point = start + (end - start) * fraction
            send(MotionEvent.ACTION_MOVE, point.x, point.y)
        }
        beforeRelease()
        send(release, end.x, end.y)
        instrumentation.waitForIdleSync()
    }
}
