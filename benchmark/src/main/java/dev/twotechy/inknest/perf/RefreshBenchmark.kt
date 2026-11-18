package dev.twotechy.inknest.perf

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class RefreshBenchmark {
    @get:Rule val benchmark = MacrobenchmarkRule()
    private val journey = createJourney()

    @Test fun libraryRefresh() = measure("refresh-library", "Library") {
        journey.control.refreshLibrary()
    }

    @Test fun statisticsRefresh() = measure("refresh-statistics", "Stats") {
        journey.control.refreshStatistics()
    }

    @Test fun clearCacheAndRefresh() = measure("clear", "Settings") {
        journey.control.clear()
    }

    private fun measure(operation: String, tab: String, refresh: () -> Unit) {
        benchmark.measureRepeated(
            packageName = TARGET,
            metrics = listOf(
                TraceSectionMetric("InkNest.control.$operation", TraceSectionMetric.Mode.Sum),
            ),
            compilationMode = CompilationMode.None(),
            iterations = 1,
            setupBlock = {
                startActivityAndWait(homeIntent())
                journey.awaitHome()
                journey.control.awaitIdle()
                journey.control.validate()
                journey.tab(tab)
            },
        ) {
            refresh()
            saveMemory(operation)
        }
    }

    companion object {
        @JvmStatic @BeforeClass
        fun prepareDataset() = createJourney().prepare()

        private fun createJourney() = BenchmarkJourney(
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
        )
    }
}
