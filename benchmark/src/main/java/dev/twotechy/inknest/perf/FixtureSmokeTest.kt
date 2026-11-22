package dev.twotechy.inknest.perf

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Test

class FixtureSmokeTest {
    @Test fun controlProviderRejectsUnprivilegedCalls() {
        val resolver = InstrumentationRegistry.getInstrumentation().context.contentResolver
        assertThrows(SecurityException::class.java) {
            resolver.call(
                Uri.parse("content://$TARGET.benchmark.control/status"),
                "state",
                null,
                null,
            )
        }
    }

    @Test fun datasetAndNavigationAreReady() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val journey = BenchmarkJourney(device)
        journey.prepare()
        journey.navigate()
        val context = InstrumentationRegistry.getInstrumentation().context
        val screenshots = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        listOf("Home", "Library", "Apps", "Stats", "Settings").forEach { tab ->
            journey.tab(tab)
            val content = when (tab) {
                "Library" -> By.text(journey.libraryCount)
                "Apps" -> By.textStartsWith("All apps")
                "Stats" -> By.text("Reading totals")
                "Settings" -> By.text("About")
                else -> By.text("Now reading")
            }
            check(device.wait(Until.hasObject(content), 10_000))
            device.waitForIdle()
            check(device.takeScreenshot(File(screenshots, "$tab.png")))
        }
        click(device, "About")
        click(device, "Review setup")
        click(device, "Continue")
        check(device.wait(Until.hasObject(By.text("Reading setup")), 10_000))
        check(device.takeScreenshot(File(screenshots, "Setup.png")))
        context.startActivity(homeIntent())
        journey.awaitHome()
        journey.tab("Settings")
        click(device, "Appearance")
        context.startActivity(homeIntent())
        journey.awaitHome()
    }

    @Test fun changedSourceAndClearRebuildRealCaches() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val journey = BenchmarkJourney(device)
        journey.prepare()
        val previous = journey.prepareReturn(changed = true)
        InstrumentationRegistry.getInstrumentation().context.startActivity(homeIntent())
        journey.awaitHome()
        journey.control.awaitIdle(refreshAfter = previous)
        journey.control.validate(changed = true)
        journey.prepareReturn(changed = false)
        InstrumentationRegistry.getInstrumentation().context.startActivity(homeIntent())
        journey.awaitHome()
        journey.control.awaitIdle()
        journey.tab("Library")
        journey.control.clear()
        journey.control.validate()
        journey.scrollLibrary()
        journey.control.validate()
    }

    private fun click(device: UiDevice, text: String) {
        checkNotNull(device.wait(Until.findObject(By.text(text)), 10_000)).click()
    }
}
