package dev.twotechy.inknest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ContentGestureGate {
    private val blockers = mutableIntStateOf(0)
    val enabled: Boolean get() = blockers.intValue == 0
    fun block() {
        blockers.intValue++
    }
    fun unblock() {
        blockers.intValue--
    }
}

internal val LocalContentGestureGate = staticCompositionLocalOf<ContentGestureGate?> { null }

@Composable
internal fun BlockContentGestures(blocked: Boolean = true) {
    val gate = LocalContentGestureGate.current
    DisposableEffect(gate, blocked) {
        if (blocked) gate?.block()
        onDispose { if (blocked) gate?.unblock() }
    }
}

internal val LocalContentGestureSession =
    staticCompositionLocalOf<StateFlow<Long>> { MutableStateFlow(0L) }
