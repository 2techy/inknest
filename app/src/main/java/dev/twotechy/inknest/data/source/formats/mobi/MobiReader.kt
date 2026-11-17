package dev.twotechy.inknest.data.source.formats.mobi

import dev.twotechy.inknest.data.source.formats.BookMetadataProbeResult
import dev.twotechy.inknest.data.source.formats.InvalidBookMetadata
import dev.twotechy.inknest.data.source.formats.optionalCover
import dev.twotechy.inknest.data.source.formats.plainDescription
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

// Reads only PalmDB directory, MOBI/EXTH headers and explicitly referenced artwork.
internal class MobiReader {
    suspend fun read(file: File, cover: suspend (ByteArray) -> Unit): BookMetadataProbeResult =
        RandomAccessFile(file, "r").use { input ->
            val records = PalmRecords(input)
            val first = header(records, 0)
            if (first.encrypted) return BookMetadataProbeResult(null, null, false)
            val second = secondary(records, first)
            if (second?.encrypted == true) return BookMetadataProbeResult(null, null, false)
            val primary = second ?: first
            var found = artwork(records, primary, first, cover)
            if (!found && primary !== first) found = artwork(records, first, first, cover)
            result(primary, first, found)
        }

    private fun secondary(records: PalmRecords, first: MobiHeader): MobiHeader? {
        val index = first.index(121) ?: return null
        if (index !in 1 until records.count) return null
        return header(records, index)
    }

    private fun result(
        primary: MobiHeader,
        first: MobiHeader,
        found: Boolean,
    ): BookMetadataProbeResult = BookMetadataProbeResult(
        primary.title ?: first.title,
        primary.text(100) ?: first.text(100),
        found,
        plainDescription(primary.text(103) ?: first.text(103)),
    )

    private fun header(records: PalmRecords, index: Int): MobiHeader {
        val bytes = records.read(index, 2 * 1024 * 1024)
        val length = validateHeader(bytes)
        val encoding = when (bytes.uint(28)) {
            65001L -> Charsets.UTF_8
            1252L -> Charset.forName("windows-1252")
            else -> null
        }
        val fields = exth(bytes, (length + 16).toInt())
        return MobiHeader(bytes, fields, encoding, fullName(bytes, encoding))
    }

    private fun validateHeader(bytes: ByteArray): Long {
        if (bytes.size < 132 || bytes.copyOfRange(16, 20).toString(Charsets.US_ASCII) != "MOBI") {
            throw InvalidBookMetadata()
        }
        val length = bytes.uint(20)
        if (length < 116 || length + 16 > bytes.size) throw InvalidBookMetadata()
        return length
    }

    private fun fullName(bytes: ByteArray, encoding: Charset?): String? {
        if (encoding == null) return null
        val start = bytes.uint(84)
        val length = bytes.uint(88)
        if (start + length > bytes.size) return null
        return String(
            bytes,
            start.toInt(),
            length.toInt(),
            encoding,
        ).trim().takeIf(String::isNotBlank)
    }

    private fun exth(bytes: ByteArray, start: Int): Map<Int, ByteArray> {
        if (start + 12 > bytes.size ||
            bytes.copyOfRange(start, start + 4).toString(Charsets.US_ASCII) != "EXTH"
        ) {
            return emptyMap()
        }
        val end = start + bytes.uint(start + 4)
        val count = bytes.uint(start + 8)
        if (end !in (start + 12).toLong()..bytes.size.toLong()) throw InvalidBookMetadata()
        if (count > 10_000) throw InvalidBookMetadata()
        val fields = mutableMapOf<Int, ByteArray>()
        var position = start + 12
        repeat(count.toInt()) {
            val field = exthField(bytes, position, end)
            fields.putIfAbsent(field.first, field.second)
            position += field.second.size + 8
        }
        return fields
    }

    private fun exthField(bytes: ByteArray, position: Int, end: Long): Pair<Int, ByteArray> {
        if (position + 8 > end) throw InvalidBookMetadata()
        val tag = bytes.uint(position).toInt()
        val size = bytes.uint(position + 4)
        if (size < 8) throw InvalidBookMetadata()
        if (position + size > end) throw InvalidBookMetadata()
        return tag to bytes.copyOfRange(position + 8, position + size.toInt())
    }

    private suspend fun artwork(
        records: PalmRecords,
        header: MobiHeader,
        fallback: MobiHeader,
        cover: suspend (ByteArray) -> Unit,
    ): Boolean {
        val base = if (header !== fallback) fallback.imageBase else header.imageBase
        for (tag in listOf(201, 202)) {
            currentCoroutineContext().ensureActive()
            val index = header.index(tag)?.toLong()?.plus(base) ?: continue
            if (!records.artworkIndex(index)) continue
            if (optionalCover(
                    records.read(index.toInt(), 8 * 1024 * 1024),
                    cover,
                )
            ) {
                return true
            }
        }
        return false
    }
}

private class MobiHeader(
    private val bytes: ByteArray,
    private val fields: Map<Int, ByteArray>,
    private val encoding: Charset?,
    private val fullName: String?,
) {
    val encrypted = bytes[12].toInt() != 0 || bytes[13].toInt() != 0
    val imageBase = bytes.uint(108)
    val title get() = text(503) ?: fullName
    fun text(tag: Int): String? = encoding?.let { charset ->
        fields[tag]?.toString(charset)?.trim()?.takeIf(String::isNotEmpty)
    }
    fun index(tag: Int): Int? = fields[tag]?.takeIf { it.size == 4 }?.uint(0)
        ?.takeIf { it < Int.MAX_VALUE }?.toInt()
}

private fun ByteArray.uint(offset: Int): Long {
    if (offset < 0 || offset.toLong() + 4 > size) throw InvalidBookMetadata()
    return (0..3).fold(0L) { value, i -> (value shl 8) or (this[offset + i].toLong() and 255) }
}
