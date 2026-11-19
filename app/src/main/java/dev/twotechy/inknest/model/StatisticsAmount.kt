package dev.twotechy.inknest.model

data class StatisticsAmount(val seconds: Long = 0, val pages: Long? = 0) {
    fun value(unit: StatisticsUnit): Long? = when (unit) {
        StatisticsUnit.Time -> seconds
        StatisticsUnit.Pages -> pages
    }

    operator fun plus(other: StatisticsAmount) = StatisticsAmount(
        seconds + other.seconds,
        if (pages == null || other.pages == null) null else pages + other.pages,
    )
}
