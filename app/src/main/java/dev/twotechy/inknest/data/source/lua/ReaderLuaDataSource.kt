package dev.twotechy.inknest.data.source.lua

import java.io.InputStream
import org.luaj.vm2.LuaValue

internal data class HistoryEntryDto(val file: String, val time: Long)

internal class ReaderLuaDataSource(private val loader: BoundedLuaLoader = BoundedLuaLoader()) {
    suspend fun lastFile(input: InputStream): String? = loader.read(input) { table ->
        val value = table.rawget("lastfile")
        if (value.isnil()) null else strictPath(value)
    }

    suspend fun history(input: InputStream): List<HistoryEntryDto> = loader.read(input) { table ->
        val keys = table.keys()
        if (keys.size > 100_000 || keys.any { !it.isinttype() || it.toint() < 1 }) invalid()
        val ordered = keys.map { it.toint() }.sorted()
        if (ordered != (1..ordered.size).toList()) invalid()
        ordered.map { index ->
            val entry = table.rawget(index)
            if (!entry.istable()) invalid()
            val time = entry.rawget("time")
            if (time.type() != LuaValue.TNUMBER || !time.todouble().isFinite() ||
                time.todouble() < 0 || time.todouble() >= Long.MAX_VALUE.toDouble() ||
                time.todouble() != time.tolong().toDouble()
            ) {
                invalid()
            }
            HistoryEntryDto(strictPath(entry.rawget("file")), time.tolong())
        }
    }

    private fun strictPath(value: LuaValue): String {
        if (value.type() != LuaValue.TSTRING) invalid()
        val path = value.tojstring()
        if (path.isBlank() || '\u0000' in path) invalid()
        return path
    }
    private fun invalid(): Nothing = throw LuaSourceException(LuaFailure.InvalidData)
}
