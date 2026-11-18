package dev.twotechy.inknest.perf

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.twotechy.inknest.benchmarkfixture.BenchmarkFixture
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
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
            setupBlock = {
                pressHome()
            },
        ) {
            startActivityAndWait(homeIntent())
            journey.awaitHome()
            journey.control.awaitIdle()
            journey.control.validate()
            saveMemory("startup-$mode")
        }
    }

    @Test fun homeReturnUnchanged() = homeReturn(changed = false)

    @Test fun homeReturnChanged() = homeReturn(changed = true)

    private fun homeReturn(changed: Boolean) {
        var previous = 0
        benchmark.measureRepeated(
            packageName = TARGET,
            metrics = listOf(
                TraceSectionMetric(BenchmarkFixture.REFRESH_TRACE, TraceSectionMetric.Mode.Sum),
            ),
            compilationMode = CompilationMode.None(),
            iterations = 5,
            setupBlock = { previous = journey.prepareReturn(changed) },
        ) {
            startActivityAndWait(homeIntent())
            journey.awaitHome()
            journey.control.awaitIdle(refreshAfter = previous)
            journey.control.validate(changed)
            saveMemory("home-return-$changed")
        }
    }

    @Test fun libraryScanAndEnrich() = libraryScenario(scan = true)

    @Test fun libraryGridScroll() = libraryScenario(scan = false)

    private fun libraryScenario(scan: Boolean) {
        val metrics = buildList {
            add(FrameTimingMetric())
            if (scan) {
                add(TraceSectionMetric(BenchmarkFixture.SCAN_TRACE, TraceSectionMetric.Mode.Sum))
            }
        }
        benchmark.measureRepeated(
            packageName = TARGET,
            metrics = metrics,
            compilationMode = CompilationMode.None(),
            iterations = 5,
            setupBlock = {
                startActivityAndWait(homeIntent())
                journey.awaitHome()
                journey.awaitRefresh()
                journey.control.validate()
                journey.tab("Library")
            },
        ) {
            if (scan) journey.control.clear() else journey.scrollLibrary()
            saveMemory(if (scan) "library-scan" else "library-scroll")
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
