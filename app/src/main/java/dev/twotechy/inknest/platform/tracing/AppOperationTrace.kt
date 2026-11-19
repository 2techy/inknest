package dev.twotechy.inknest.platform.tracing

import android.os.Build
import android.os.Trace

internal object AppOperationTrace {
    val tracer = OperationTracer(
        beginSection = { name, cookie ->
            if (Build.VERSION.SDK_INT >= 29) Trace.beginAsyncSection(name, cookie)
        },
        endSection = { name, cookie ->
            if (Build.VERSION.SDK_INT >= 29) Trace.endAsyncSection(name, cookie)
        },
    )
}
