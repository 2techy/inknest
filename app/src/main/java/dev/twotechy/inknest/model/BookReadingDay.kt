package dev.twotechy.inknest.model

import java.time.LocalDate

data class BookReadingDay(val date: LocalDate, val seconds: Long, val pages: Long?)
