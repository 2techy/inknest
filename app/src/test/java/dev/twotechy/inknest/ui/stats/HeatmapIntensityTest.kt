package dev.twotechy.inknest.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeatmapIntensityTest {
    @Test fun relativeScaleIgnoresEmptyUnknownDaysAndKeepsSparseActivityVisible() {
        val active = listOf(1L, 2L, 3L, 4L)
        val scale = HeatmapIntensity(active + List(100) { 0L } + null)
        assertEquals(listOf(1, 2, 3, 4), active.map(scale::bucket))
        assertEquals(active.map(HeatmapIntensity(active)::bucket), active.map(scale::bucket))
        assertEquals(0, scale.bucket(0))
        assertNull(scale.bucket(null))
        assertEquals(0, HeatmapIntensity(emptyList()).bucket(0))
        assertEquals(3, HeatmapIntensity(listOf(4, 4, 0, null)).bucket(4))
        val sparse = HeatmapIntensity(listOf(29, 219))
        assertEquals(1, sparse.bucket(29))
        assertEquals(4, sparse.bucket(219))
    }
}
