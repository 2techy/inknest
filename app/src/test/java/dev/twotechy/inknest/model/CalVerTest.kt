package dev.twotechy.inknest.model

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalVerTest {
    @Test fun comparesNumbersAcrossMonthAndYearBoundaries() {
        assertTrue(CalVer.parse("2026.11.10")!! > CalVer.parse("2026.11.9")!!)
        assertTrue(CalVer.parse("2026.11.0")!! > CalVer.parse("2026.9.99")!!)
        assertTrue(CalVer.parse("2027.1.0")!! > CalVer.parse("2026.12.99")!!)
        assertTrue(CalVer.parse("2026.11.0")!! < CalVer.parse("2027.1.0")!!)
    }

    @Test fun rejectsLocalVersionsAndMalformedTags() {
        listOf(
            "0.1.0-dev",
            "2026.11.0-beta",
            "2026.0.0",
            "2026.13.0",
            "v2026.11.0",
            "2026.11.999999999999",
            "2026.11.-1",
        ).forEach { assertNull(CalVer.parse(it)) }
    }
}
