package dev.twotechy.inknest.perf

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val benchmark = MacrobenchmarkRule()
    private val journey =
        BenchmarkJourney(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()))

    @Before fun prepareDataset() = journey.prepare()

    @Test fun coldStartup() = startup(StartupMode.COLD)

    @Test fun warmStartup() = startup(StartupMode.WARM)

    private fun startup(mode: StartupMode) {
        benchmark.measureRepeated(
            packageName = TARGET,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = mode,
            iterations = 5,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait(homeIntent())
            journey.awaitHome()
            saveMemory("startup-$mode")
        }
    }

    @Test fun mainTabNavigation() {
        benchmark.measureRepeated(
            packageName = TARGET,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                startActivityAndWait(homeIntent())
                journey.awaitHome()
                journey.awaitRefresh()
            },
        ) {
            journey.navigate()
            saveMemory("navigation")
        }
    }
}
