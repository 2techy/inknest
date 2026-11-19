package dev.twotechy.inknest.platform.tracing

internal data class OperationTraceState(
    val started: Int = 0,
    val completed: Int = 0,
    val active: Int = 0,
)

// Cookies identify spans across coroutine suspension and overlapping operations.
