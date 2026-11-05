package dev.twotechy.inknest

import android.os.StrictMode

internal fun configureRuntimeDiagnostics() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork()
            .penaltyLog().build(),
    )
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectActivityLeaks()
            .penaltyLog().build(),
    )
}
