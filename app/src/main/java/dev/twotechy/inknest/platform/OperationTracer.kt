package dev.twotechy.inknest.platform

internal data class OperationTraceState(
    val started: Int = 0,
    val completed: Int = 0,
    val active: Int = 0,
)

// Cookies identify spans across coroutine suspension and overlapping operations.
internal class OperationTracer(
    private val beginSection: (String, Int) -> Unit = { _, _ -> },
    private val endSection: (String, Int) -> Unit = { _, _ -> },
) {
    private val lock = Any()
    private var cookie = 0
    private val states = mutableMapOf<String, OperationTraceState>()

    fun begin(name: String): AutoCloseable = synchronized(lock) {
        val token = ++cookie
        val previous = states[name] ?: OperationTraceState()
        states[name] = previous.copy(started = previous.started + 1, active = previous.active + 1)
        beginSection(name, token)
        var closed = false
        AutoCloseable {
            synchronized(lock) {
                if (!closed) {
                    closed = true
                    endSection(name, token)
                    val current = checkNotNull(states[name])
                    states[name] = current.copy(
                        completed = current.completed + 1,
                        active = current.active - 1,
                    )
                }
            }
        }
    }

    fun snapshot(name: String): OperationTraceState = synchronized(lock) {
        states[name] ?: OperationTraceState()
    }

    companion object {
        const val FOREGROUND_REFRESH = "InkNest.foregroundRefresh"
        const val LIBRARY_SCAN = "InkNest.libraryScan"
    }
}
