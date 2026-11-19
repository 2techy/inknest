package dev.twotechy.inknest.data.source.koreader.lua

import java.io.InputStream
import kotlin.math.roundToInt
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

internal class KoreaderLuaParser(private val loader: BoundedLuaLoader = BoundedLuaLoader()) {
    suspend fun lastFile(input: InputStream): String? = loader.read(input) { table ->
        val value = table.rawget("lastfile")
        if (value.isnil()) null else strictPath(value)
    }

    suspend fun history(input: InputStream): List<HistoryEntryDto> = loader.read(input) { table ->
        historyIndices(table).map { historyEntry(table.rawget(it)) }
    }

    // A malformed row cannot discard independent valid entries in an otherwise valid document.
    suspend fun trackedHistory(input: InputStream): TrackedHistoryDto =
        loader.read(input) { table ->
            var skipped = 0
            val entries = historyIndices(table).mapNotNull { index ->
                try {
                    historyEntry(table.rawget(index))
                } catch (error: LuaSourceException) {
                    if (error.reason != LuaFailure.InvalidData) throw error
                    skipped++
                    null
                }
            }
            TrackedHistoryDto(entries, skipped)
        }

    private fun historyIndices(table: LuaTable): List<Int> {
        val keys = table.keys()
        if (keys.size > 100_000) invalid()
        val ordered = keys.map { key ->
            if (!key.isinttype()) invalid()
            val index = key.toint()
            if (index < 1) invalid()
            index
        }.sorted()
        if (ordered != (1..ordered.size).toList()) invalid()
        return ordered
    }

    private fun historyEntry(entry: LuaValue): HistoryEntryDto {
        if (!entry.istable()) invalid()
        val time = historyTimestamp(entry.rawget("time"))
        return HistoryEntryDto(strictPath(entry.rawget("file")), time)
    }

    private fun historyTimestamp(value: LuaValue): Long {
        if (value.type() != LuaValue.TNUMBER) invalid()
        val seconds = value.todouble()
        if (!seconds.isFinite()) invalid()
        if (seconds !in 0.0..(Long.MAX_VALUE / 1000).toDouble()) invalid()
        if (seconds != value.tolong().toDouble()) invalid()
        return value.tolong()
    }

    suspend fun sidecar(input: InputStream): SidecarDto = loader.read(input, ::parseSidecar)

    private fun parseSidecar(table: LuaTable): SidecarDto {
        val percent = sidecarPercent(table.rawget("percent_finished"))
        val total = percent?.let { positivePageCount(table.rawget("doc_pages")) }
        val current = if (percent != null && total != null) (percent * total).roundToInt() else null
        return SidecarDto(
            percent,
            sidecarStatus(table.rawget("summary")),
            current,
            total,
        )
    }

    private fun sidecarPercent(value: LuaValue): Double? {
        if (value.isnil()) return null
        if (value.type() != LuaValue.TNUMBER) invalid()
        val percent = value.todouble()
        if (!percent.isFinite()) invalid()
        if (percent !in 0.0..1.0) invalid()
        return percent
    }

    private fun sidecarStatus(summary: LuaValue): String? {
        if (summary.isnil()) return null
        if (!summary.istable()) invalid()
        val status = summary.rawget("status")
        if (status.isnil()) return null
        if (status.type() != LuaValue.TSTRING) invalid()
        return status.tojstring()
    }

    private fun positivePageCount(value: LuaValue): Int? {
        if (value.type() != LuaValue.TNUMBER) return null
        val number = value.todouble()
        return number.takeIf {
            it.isFinite() && it in 1.0..Int.MAX_VALUE.toDouble() && it % 1 == 0.0
        }
            ?.toInt()
    }

    private fun strictPath(value: LuaValue): String {
        if (value.type() != LuaValue.TSTRING) invalid()
        val path = value.tojstring()
        if (path.isBlank() || '\u0000' in path) invalid()
        return path
    }
    private fun invalid(): Nothing = throw LuaSourceException(LuaFailure.InvalidData)
}
