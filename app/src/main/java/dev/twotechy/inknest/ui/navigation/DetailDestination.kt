package dev.twotechy.inknest.ui.navigation

import dev.twotechy.inknest.model.ReadingItemId
import java.time.LocalDate

internal sealed interface DetailDestination {
    val savedParts: List<String>

    data class Book(val id: ReadingItemId) : DetailDestination {
        override val savedParts = listOf("book", id.sourceId, id.path)
    }

    data class StatsDay(val date: LocalDate) : DetailDestination {
        override val savedParts = listOf("day", date.toString(), "")
    }

    companion object {
        fun restore(parts: List<String>): DetailDestination? = when (parts.firstOrNull()) {
            "book" -> if (parts.size == 3) Book(ReadingItemId(parts[1], parts[2])) else null

            "day" -> parts.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let(::StatsDay)

            else -> null
        }
    }
}
