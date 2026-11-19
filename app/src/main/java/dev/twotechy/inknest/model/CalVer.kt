package dev.twotechy.inknest.model

internal data class CalVer(val year: Int, val month: Int, val patch: Int) : Comparable<CalVer> {
    override fun compareTo(other: CalVer): Int =
        compareValuesBy(this, other, CalVer::year, CalVer::month, CalVer::patch)

    companion object {
        fun parse(value: String): CalVer? {
            val match = Regex("(\\d{4})\\.(\\d{1,2})\\.(0|[1-9]\\d*)").matchEntire(value)
                ?: return null
            val (year, month, patch) = match.destructured
            val result = CalVer(year.toInt(), month.toInt(), patch.toIntOrNull() ?: return null)
            return result.takeIf { it.year > 0 && it.month in 1..12 }
        }
    }
}
