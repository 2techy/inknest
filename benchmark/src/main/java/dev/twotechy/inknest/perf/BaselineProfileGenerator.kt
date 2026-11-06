package dev.twotechy.inknest.perf

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val profile = BaselineProfileRule()
    private val journey =
        BenchmarkJourney(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()))

    @Before fun prepareDataset() = journey.prepare()

    @Test fun startup() {
        profile.collect(
            packageName = TARGET,
            maxIterations = 3,
            stableIterations = 2,
            outputFilePrefix = "inknest-startup",
            includeInStartupProfile = true,
            filterPredicate = ::appRule,
        ) {
            pressHome()
            startActivityAndWait(homeIntent())
            journey.awaitHome()
        }
    }

    @Test fun navigation() {
        profile.collect(
            packageName = TARGET,
            maxIterations = 3,
            stableIterations = 2,
            outputFilePrefix = "inknest-navigation",
            filterPredicate = ::appRule,
        ) {
            startActivityAndWait(homeIntent())
            journey.awaitHome()
            journey.navigate()
        }
    }

    private fun appRule(rule: String): Boolean =
        rule.contains("Ldev/twotechy/inknest/") && !rule.contains("/benchmarkfixture/")
}
