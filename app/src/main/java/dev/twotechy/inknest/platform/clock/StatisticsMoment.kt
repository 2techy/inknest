package dev.twotechy.inknest.platform.clock

import java.time.LocalDate
import java.time.ZoneId

internal data class StatisticsMoment(val today: LocalDate, val zone: ZoneId)
