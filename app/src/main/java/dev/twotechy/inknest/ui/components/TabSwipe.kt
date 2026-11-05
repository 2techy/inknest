package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.ui.navigation.TopLevelDestination
import dev.twotechy.inknest.ui.theme.InkGestureTokens

@Composable
internal fun Modifier.tabSwipe(
    selected: TopLevelDestination,
    enabled: Boolean,
    onSelect: (TopLevelDestination) -> Unit,
): Modifier {
    val density = LocalDensity.current
    val gate = LocalContentGestureGate.current
    val allowed =
        rememberUpdatedState(
            enabled && gate?.enabled != false && WindowInsets.ime.getBottom(density) == 0,
        )
    val edges = systemGestureEdges()
    val threshold = with(density) { InkGestureTokens.tabThreshold.toPx() }
    val select = rememberUpdatedState(onSelect)
    val session = LocalContentGestureSession.current
    val generation by session.collectAsStateWithLifecycle()
    return pointerInput(selected, edges, generation) {
        awaitEachGesture {
            val startedSession = session.value
            val down = awaitFirstDown(requireUnconsumed = false)
            val withinContent = edges.contains(down.position.x, size.width)
            if (allowed.value && withinContent) {
                val offset = awaitTabOffset(threshold) {
                    allowed.value &&
                        session.value == startedSession
                }
                val target = adjacentTab(selected, offset)
                if (target != null) select.value(target)
            }
        }
    }
}

internal fun adjacentTab(selected: TopLevelDestination, offset: Int): TopLevelDestination? =
    if (offset == 0) null else TopLevelDestination.entries.getOrNull(selected.ordinal + offset)

private suspend fun AwaitPointerEventScope.awaitTabOffset(
    threshold: Float,
    enabled: () -> Boolean,
): Int {
    val drag = ContentDrag(viewConfiguration.touchSlop)
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val change = event.changes.singleOrNull() ?: return 0
        if (!enabled() || change.isConsumed) return 0
        val delta = change.position - change.previousPosition
        drag.move(delta.x, delta.y)
        if (drag.direction == DragDirection.Vertical) return 0
        if (drag.direction == DragDirection.Horizontal) change.consume()
        if (!change.pressed) return drag.tabOffset(threshold)
    }
}
