package dev.twotechy.inknest.data.source.koreader.lua

internal data class TrackedHistoryDto(val entries: List<HistoryEntryDto>, val skipped: Int = 0)
