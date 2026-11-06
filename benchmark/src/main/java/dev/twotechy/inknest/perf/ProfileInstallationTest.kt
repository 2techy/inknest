package dev.twotechy.inknest.perf

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test

class ProfileInstallationTest {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Test fun packagedProfileCompilesAndStarts() {
        val journey = BenchmarkJourney(
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
        )
        journey.prepare()
        benchmark.measureRepeated(
            packageName = TARGET,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Require,
                warmupIterations = 0,
            ),
            startupMode = StartupMode.COLD,
            iterations = 1,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait(homeIntent())
            journey.awaitHome()
        }
    }
}
