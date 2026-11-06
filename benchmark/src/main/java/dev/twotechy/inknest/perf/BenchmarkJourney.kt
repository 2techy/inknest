package dev.twotechy.inknest.perf

import android.app.KeyguardManager
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import java.util.regex.Pattern

internal const val TARGET = "dev.twotechy.inknest.benchmark"
internal const val OUTPUT = "/sdcard/Android/media/dev.twotechy.inknest.perf/benchmarks"

internal fun homeIntent() = Intent(Intent.ACTION_MAIN).apply {
    setClassName(TARGET, "dev.twotechy.inknest.MainActivity")
    addCategory(Intent.CATEGORY_HOME)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal class BenchmarkJourney(private val device: UiDevice) {
    fun prepare() {
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val context = InstrumentationRegistry.getInstrumentation().context
        check(!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            "Unlock the benchmark device before running instrumentation"
        }
        device.executeShellCommand("appops set $TARGET MANAGE_EXTERNAL_STORAGE allow")
        device.executeShellCommand(
            "am start -W -n $TARGET/dev.twotechy.inknest.benchmarkfixture.BenchmarkSetupActivity",
        )
        if (!device.wait(Until.hasObject(By.text("Benchmark ready")), 2_000)) {
            selectFixtureFolder()
        }
        check(device.wait(Until.hasObject(By.text("Benchmark ready")), 30_000))
        device.executeShellCommand(
            "am start -W -n $TARGET/dev.twotechy.inknest.MainActivity -a android.intent.action.MAIN -c android.intent.category.HOME",
        )
        awaitHome()
        tab("Library")
        check(device.wait(Until.hasObject(By.text("All (100)")), 30_000))
        tab("Stats")
        check(device.wait(Until.hasObject(By.text("Reading totals")), 30_000))
        awaitRefresh()
        tab("Home")
    }

    private fun selectFixtureFolder() {
        val useFolder = device.wait(
            Until.findObject(By.text(Pattern.compile("Use this folder", Pattern.CASE_INSENSITIVE))),
            3_000,
        )
        if (useFolder != null) {
            useFolder.click()
            device.wait(Until.findObject(By.res("android:id/button1")), 5_000)?.click()
        } else {
            // Tango's DocumentsUI ignores EXTRA_INITIAL_URI and starts at storage root.
            openOemFolder("Download")
            openOemFolder("InkNestBenchmark")
            checkNotNull(device.wait(Until.findObject(By.text("Select")), 5_000)).click()
            device.wait(Until.findObject(By.text("Confirm")), 5_000)?.click()
        }
    }

    private fun openOemFolder(name: String) {
        val item = By.res("com.android.documentsui:id/tv_doc_manager_item_name").text(name)
        checkNotNull(device.wait(Until.findObject(item), 10_000)) {
            "The OEM picker did not show benchmark folder $name"
        }.click()
        device.waitForIdle()
    }

    fun awaitHome() {
        check(device.wait(Until.hasObject(By.text("Benchmark Book 001")), 30_000))
    }

    fun navigate() {
        tab("Library")
        check(device.wait(Until.hasObject(By.text("All (100)")), 10_000))
        tab("Apps")
        check(device.wait(Until.hasObject(By.textStartsWith("All apps")), 10_000))
        tab("Stats")
        check(device.wait(Until.hasObject(By.text("Reading totals")), 10_000))
        tab("Home")
        awaitHome()
    }

    fun tab(name: String) {
        check(device.wait(Until.hasObject(By.text(name)), 10_000))
        checkNotNull(device.findObjects(By.text(name)).lastOrNull()).click()
        device.waitForIdle()
    }

    fun awaitRefresh() {
        check(device.wait(Until.gone(By.text("Refreshing…")), 30_000))
    }
}

internal fun MacrobenchmarkScope.saveMemory(label: String) {
    val text = device.executeShellCommand("dumpsys meminfo $TARGET")
    val directory = File(
        InstrumentationRegistry.getInstrumentation().context.getExternalFilesDir(null),
        "memory",
    )
        .apply { mkdirs() }
    File(directory, "$label-${iteration ?: 0}.txt").writeText(text)
}
