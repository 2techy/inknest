package dev.twotechy.inknest.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentGesturesTest {
    @Test fun horizontalDirectionLockSurvivesLaterVerticalMovement() {
        val drag = ContentDrag(8f)
        drag.move(20f, 20f)
        assertEquals(0, drag.tabOffset(64f))
        drag.move(-100f, 0f)
        assertEquals(1, drag.tabOffset(64f))
        drag.move(0f, 200f)
        assertEquals(DragDirection.Horizontal, drag.direction)
    }

    @Test fun allTabsWrapInBothDirectionsAndSettingsIsExcluded() {
        val tabs = listOf(
            TopLevelDestination.Home,
            TopLevelDestination.Library,
            TopLevelDestination.Stats,
            TopLevelDestination.Apps,
        )
        tabs.forEachIndexed { index, tab ->
            assertEquals(tabs[(index + 1) % tabs.size], adjacentTab(tab, 1))
            assertEquals(tabs[(index + tabs.size - 1) % tabs.size], adjacentTab(tab, -1))
        }
        assertNull(adjacentTab(TopLevelDestination.Settings, -1))
        assertNull(adjacentTab(TopLevelDestination.Home, 0))
    }

    @Test fun pullMustBeginAtTopAndCrossThresholdBeforeRelease() {
        val state = PullRefreshState()
        state.start(8f)
        state.move(Offset(0f, 160f))
        state.onPostScroll(Offset(0f, 20f), Offset(0f, 140f), NestedScrollSource.UserInput)
        assertFalse(state.finish(80f))
        state.start(8f)
        state.move(Offset(0f, 120f))
        state.onPostScroll(Offset.Zero, Offset(0f, 120f), NestedScrollSource.UserInput)
        state.onPreScroll(Offset(0f, -60f), NestedScrollSource.UserInput)
        assertFalse(state.finish(80f))
        state.start(8f)
        state.move(Offset(0f, 100f))
        state.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput)
        assertTrue(state.finish(80f))
        assertFalse(state.finish(80f))
    }

    @Test fun horizontalCancelAndFlingCannotRefresh() {
        val state = PullRefreshState()
        state.start(8f)
        state.move(Offset(120f, 100f))
        state.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput)
        assertFalse(state.finish(80f))
        state.start(8f)
        state.move(Offset(0f, 100f))
        state.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.SideEffect)
        assertFalse(state.finish(80f))
        state.start(8f)
        state.move(Offset(0f, 100f))
        state.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput)
        state.reset()
        assertFalse(state.finish(80f))
    }
}
