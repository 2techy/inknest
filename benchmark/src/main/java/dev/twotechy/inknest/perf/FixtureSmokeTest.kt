package dev.twotechy.inknest.perf

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import org.junit.Test

class FixtureSmokeTest {
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
                "Library" -> By.text("All (100)")
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
        click(device, "Next")
        check(device.wait(Until.hasObject(By.text("Your reading space")), 10_000))
        check(device.takeScreenshot(File(screenshots, "Setup.png")))
        context.startActivity(homeIntent())
        journey.awaitHome()
        journey.tab("Settings")
        click(device, "Appearance")
        context.startActivity(homeIntent())
        journey.awaitHome()
    }

    private fun click(device: UiDevice, text: String) {
        checkNotNull(device.wait(Until.findObject(By.text(text)), 10_000)).click()
    }
}
