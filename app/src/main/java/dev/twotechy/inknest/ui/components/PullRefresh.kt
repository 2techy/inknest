package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkGestureTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

internal data class RefreshAction(
    val enabled: Boolean,
    val running: Boolean,
    val refresh: () -> Unit,
) {
    fun canRefresh(): Boolean = enabled && !running
}

@Composable
internal fun PullRefresh(action: RefreshAction?, content: @Composable BoxScope.() -> Unit) {
    if (action == null) {
        Box(Modifier.fillMaxSize(), content = content)
        return
    }
    val state = remember { PullRefreshState() }
    val density = LocalDensity.current
    val threshold = with(density) { InkGestureTokens.refreshThreshold.toPx() }
    val gate = LocalContentGestureGate.current
    val enabled =
        rememberUpdatedState(
            action.canRefresh() && gate?.enabled != false &&
                WindowInsets.ime.getBottom(density) == 0,
        )
    val refresh = rememberUpdatedState(action.refresh)
    val session = LocalContentGestureSession.current
    val generation by session.collectAsStateWithLifecycle()
    val edges = systemGestureEdges()
    Box(
        Modifier.fillMaxSize().nestedScroll(state)
            // A zero-consuming scrollable lets short/empty content participate in nested scroll.
            .scrollable(
                rememberScrollableState {
                    0f
                },
                Orientation.Vertical,
                overscrollEffect = null,
            )
            .pointerInput(state, generation, edges) {
                awaitEachGesture {
                    val startedSession = session.value
                    val down =
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (enabled.value && edges.contains(down.position.x, size.width)) {
                        state.start(viewConfiguration.touchSlop)
                        try {
                            if (awaitRefreshRelease(state, threshold) {
                                    enabled.value &&
                                        session.value == startedSession
                                }
                            ) {
                                refresh.value.invoke()
                            }
                        } finally {
                            state.reset()
                        }
                    }
                }
            },
    ) {
        content()
        val label = when {
            action.running -> R.string.refreshing
            enabled.value && state.distance >= threshold -> R.string.gesture_release_refresh
            else -> null
        }
        if (label != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                },
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(InkBorder.default, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    stringResource(label),
                    Modifier.padding(InkSpacing.sm),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitRefreshRelease(
    state: PullRefreshState,
    threshold: Float,
    enabled: () -> Boolean,
): Boolean {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.singleOrNull() ?: return false
        if (!enabled()) return false
        state.move(change.position - change.previousPosition)
        awaitPointerEvent(PointerEventPass.Final)
        if (!change.pressed) return !change.isConsumed && state.finish(threshold)
    }
}
