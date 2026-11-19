package dev.twotechy.inknest.perf

import android.Manifest
import android.net.Uri
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.benchmarkfixture.BenchmarkFixture

internal class BenchmarkControl {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uri = Uri.parse("content://$TARGET.benchmark.control/status")

    fun snapshot(): BenchmarkOperations = withShellIdentity {
        val state = checkNotNull(
            instrumentation.context.contentResolver.call(
                uri,
                "state",
                null,
                null,
            ),
        )
        BenchmarkOperations(
            state.getInt("refreshes"),
            state.getInt("indexes"),
            state.getInt("active"),
        )
    }

    fun awaitIdle(refreshAfter: Int? = null) {
        val deadline = SystemClock.uptimeMillis() + BenchmarkFixture.TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            val current = snapshot()
            val fresh = refreshAfter?.let { current.refreshes > it } ?: true
            if (current.active == 0 && fresh) {
                return
            }
            SystemClock.sleep(100)
        }
        error("Benchmark operations did not complete after $refreshAfter: ${snapshot()}")
    }

    fun validate(changed: Boolean = false) = call("validate", changed)
    fun prepare() = call("prepare", changed = false)
    fun clear() = call("clear", changed = false)
    fun refreshLibrary() = call("refresh-library", changed = false)
    fun refreshStatistics() = call("refresh-statistics", changed = false)

    private fun call(method: String, changed: Boolean) = withShellIdentity {
        val result = instrumentation.context.contentResolver.call(
            uri,
            method,
            if (changed) "changed" else "baseline",
            null,
        )
        check(result?.getBoolean("valid") == true)
    }

    private fun <T> withShellIdentity(block: () -> T): T {
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.DUMP)
        return try {
            block()
        } finally {
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
