package dev.twotechy.inknest.ui.stats

// Only known, active days set the relative scale. Empty/unknown days never dilute it.
internal class HeatmapIntensity(values: List<Long?>) {
    private val active = values.filterNotNull().filter { it > 0 }.sorted()
    private val thresholds = listOf(1, 2, 3).map { quartile ->
        active.getOrNull((active.size - 1).coerceAtLeast(0) * quartile / 4)
    }

    fun bucket(value: Long?): Int? = when {
        value == null -> null

        value <= 0 -> 0

        active.isEmpty() -> 0

        // A single activity level should still stand out from the empty calendar.
        active.first() == active.last() -> 3

        else -> (thresholds.indexOfFirst { value <= checkNotNull(it) } + 1)
            .takeIf { it > 0 } ?: 4
    }
}
