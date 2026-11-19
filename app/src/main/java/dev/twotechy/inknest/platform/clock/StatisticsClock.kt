package dev.twotechy.inknest.platform.clock

import kotlinx.coroutines.flow.StateFlow

internal interface StatisticsClock {
    val moments: StateFlow<StatisticsMoment>
}
