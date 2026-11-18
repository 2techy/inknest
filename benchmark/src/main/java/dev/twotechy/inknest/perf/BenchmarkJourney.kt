package dev.twotechy.inknest.perf

import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.twotechy.inknest.benchmarkfixture.BenchmarkFixture
import java.io.File
import java.util.UUID
import java.util.regex.Pattern

internal const val TARGET = "dev.twotechy.inknest.benchmark"
internal const val OUTPUT = "/sdcard/Android/media/dev.twotechy.inknest.perf/benchmarks"

internal fun homeIntent() = Intent(Intent.ACTION_MAIN).apply {
    setClassName(TARGET, "dev.twotechy.inknest.MainActivity")
    addCategory(Intent.CATEGORY_HOME)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal class BenchmarkJourney(private val device: UiDevice) {
    private var fixtureRequest = ""
    private val fixtureReady get() = "Benchmark ready $fixtureRequest"
    val control = BenchmarkControl()
    val libraryCount = "All (${BenchmarkFixture.BOOKS})"
    fun prepare() {
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val context = InstrumentationRegistry.getInstrumentation().context
        check(!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            "Unlock the benchmark device before running instrumentation"
        }
        device.executeShellCommand("appops set $TARGET MANAGE_EXTERNAL_STORAGE allow")
        launchFixture("prepare")
        awaitSetupOrPicker()
        if (!device.hasObject(By.text(fixtureReady))) {
            selectFixtureFolder()
        }
        awaitFixtureReady()
        device.executeShellCommand(
            "am start -W -n $TARGET/dev.twotechy.inknest.MainActivity -a android.intent.action.MAIN -c android.intent.category.HOME",
        )
        awaitHome()
        control.awaitIdle()
        control.prepare()
        tab("Library")
        check(device.wait(Until.hasObject(By.text(libraryCount)), BenchmarkFixture.TIMEOUT_MILLIS))
        tab("Stats")
        check(device.wait(Until.hasObject(By.text("Reading totals")), 30_000))
        awaitRefresh()
        control.validate()
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
        check(
            device.wait(
                Until.hasObject(By.text(BenchmarkFixture.CURRENT_TITLE)),
                BenchmarkFixture.TIMEOUT_MILLIS,
            ),
        )
    }

    fun navigate() {
        tab("Library")
        check(device.wait(Until.hasObject(By.text(libraryCount)), 10_000))
        tab("Apps")
        check(device.wait(Until.hasObject(By.textStartsWith("All apps")), 10_000))
        tab("Stats")
        check(device.wait(Until.hasObject(By.text("Reading totals")), 10_000))
        tab("Home")
        awaitHome()
    }

    fun tab(name: String) {
        val selector = if (name == "Settings") {
            tab("Home")
            By.desc(name)
        } else {
            By.text(name)
        }
        check(device.wait(Until.hasObject(selector), 10_000))
        checkNotNull(device.findObjects(selector).lastOrNull()).click()
        device.waitForIdle()
    }

    fun awaitRefresh() {
        control.awaitIdle()
    }

    fun prepareReturn(changed: Boolean): Int {
        openSettings()
        control.awaitIdle()
        launchFixture("reset")
        awaitFixtureReady()
        val previous = control.snapshot().refreshes
        device.executeShellCommand(
            "am start -W -n $TARGET/dev.twotechy.inknest.MainActivity -a android.intent.action.MAIN -c android.intent.category.HOME",
        )
        awaitHome()
        control.awaitIdle(refreshAfter = previous)
        control.validate()
        if (changed) {
            launchFixture("mutate")
            awaitFixtureReady()
        }
        openSettings()
        return control.snapshot().refreshes
    }

    private fun openSettings() {
        device.executeShellCommand("am start -W -a android.settings.SETTINGS")
        check(device.wait(Until.gone(By.pkg(TARGET)), 10_000))
    }

    fun scrollLibrary() {
        device.waitForIdle()
        val bounds = libraryBounds()
        val before = visibleLibraryBooks()
        val top = bounds.top + bounds.height() / 4
        val bottom = bounds.bottom - bounds.height() / 4
        repeat(6) { swipe(bounds.centerX(), bottom, top) }
        check(visibleLibraryBooks() != before) { "Library gestures did not move the visible books" }
        repeat(6) { swipe(bounds.centerX(), top, bottom) }
    }

    private fun visibleLibraryBooks(): Set<String> = device.findObjects(
        By.textStartsWith("Benchmark Book ").pkg(TARGET),
    ).mapNotNull { it.text }.toSet().also {
        check(it.isNotEmpty()) { "No fixture books visible in Library" }
    }

    private fun libraryBounds(): Rect {
        repeat(3) { attempt ->
            try {
                val selector = By.scrollable(true).pkg(TARGET)
                check(device.wait(Until.hasObject(selector), 10_000))
                // Library also exposes a horizontal filter row as scrollable.
                val bounds = checkNotNull(
                    device.findObjects(selector).map {
                        it.visibleBounds
                    }.maxByOrNull { it.height() },
                )
                check(bounds.height() > device.displayHeight / 2) {
                    "Library grid unavailable: $bounds"
                }
                return bounds
            } catch (error: StaleObjectException) {
                if (attempt == 2) throw error
                device.waitForIdle()
            }
        }
        error("Library bounds unavailable")
    }

    private fun awaitSetupOrPicker() {
        val deadline = SystemClock.uptimeMillis() + BenchmarkFixture.TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.hasObject(By.text(fixtureReady))) return
            if (device.hasObject(By.pkg("com.android.documentsui"))) return
            SystemClock.sleep(100)
        }
        error("Fixture setup did not finish for $fixtureRequest")
    }

    private fun swipe(x: Int, start: Int, end: Int) {
        check(device.swipe(x, start, x, end, 40))
        device.waitForIdle()
    }

    private fun launchFixture(operation: String) {
        fixtureRequest = UUID.randomUUID().toString()
        device.executeShellCommand(
            "am start -W --activity-single-top -n $TARGET/dev.twotechy.inknest.benchmarkfixture.BenchmarkSetupActivity --es operation $operation --es request $fixtureRequest",
        )
    }

    private fun awaitFixtureReady() {
        val ready = Until.hasObject(By.text(fixtureReady))
        check(device.wait(ready, BenchmarkFixture.TIMEOUT_MILLIS))
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
