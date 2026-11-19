package dev.twotechy.inknest.platform.clock

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

internal class SystemStatisticsClock(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StatisticsClock {
    override val moments = flow {
        while (true) {
            emit(systemMoment())
            delay(60_000)
        }
    }.distinctUntilChanged().flowOn(dispatcher)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), systemMoment())
}

private fun systemMoment(): StatisticsMoment {
    val zone = ZoneId.systemDefault()
    return StatisticsMoment(LocalDate.now(zone), zone)
}
