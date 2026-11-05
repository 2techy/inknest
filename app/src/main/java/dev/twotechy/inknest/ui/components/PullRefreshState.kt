package dev.twotechy.inknest.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

internal class PullRefreshState : NestedScrollConnection {
    var distance by mutableFloatStateOf(0f)
        private set
    private var drag: ContentDrag? = null
    private var eligible: Boolean? = null

    fun start(slop: Float) {
        reset()
        drag = ContentDrag(slop)
    }

    fun move(delta: Offset) {
        drag?.move(delta.x, delta.y)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput || drag == null) return Offset.Zero
        // The first scrolling movement decides eligibility for this entire touch sequence.
        considerStart(consumed.y, available.y)
        if (eligible != true || drag?.direction != DragDirection.Vertical) return Offset.Zero
        val before = distance
        distance = (distance + available.y).coerceAtLeast(0f)
        return Offset(0f, distance - before)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (distance == 0f || available.y >= 0f) return Offset.Zero
        val consumed = available.y.coerceAtLeast(-distance)
        distance += consumed
        return Offset(0f, consumed)
    }

    private fun considerStart(consumed: Float, available: Float) {
        if (eligible != null) return
        if (consumed == 0f && available == 0f) return
        eligible = consumed == 0f && available > 0f
    }

    fun finish(threshold: Float): Boolean {
        val refresh = eligible == true && distance >= threshold
        reset()
        return refresh
    }

    fun reset() {
        distance = 0f
        eligible = null
        drag = null
    }
}
