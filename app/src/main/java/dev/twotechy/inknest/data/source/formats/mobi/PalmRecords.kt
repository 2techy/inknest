package dev.twotechy.inknest.data.source.formats.mobi

import dev.twotechy.inknest.data.source.formats.InvalidBookMetadata
import dev.twotechy.inknest.data.source.io.SourceLimitExceeded
import java.io.RandomAccessFile

internal class PalmRecords(private val input: RandomAccessFile) {
    private val offsets = directory()
    val count get() = offsets.lastIndex

    fun artworkIndex(index: Long): Boolean {
        if (index !in 0 until count.toLong()) return false
        return offsets[index.toInt() + 1] - offsets[index.toInt()] <= 8 * 1024 * 1024
    }

    fun read(index: Int, limit: Int): ByteArray {
        val length = offsets[index + 1] - offsets[index]
        if (length > limit) throw SourceLimitExceeded()
        input.seek(offsets[index])
        return ByteArray(length.toInt()).also(input::readFully)
    }
    private fun directory(): List<Long> {
        val count = recordCount()
        val offsets = List(count) {
            val offset = input.readInt().toLong() and 0xffffffffL
            input.skipBytes(4)
            offset
        } + input.length()
        if (offsets.first() < 78L + count * 8L) throw InvalidBookMetadata()
        if (offsets.zipWithNext().any { (a, b) -> a >= b }) throw InvalidBookMetadata()
        return offsets
    }
    private fun recordCount(): Int {
        if (input.length() < 78) throw InvalidBookMetadata()
        input.seek(60)
        val signature = ByteArray(8).also(input::readFully).toString(Charsets.US_ASCII)
        if (signature != "BOOKMOBI") throw InvalidBookMetadata()
        input.seek(76)
        val count = input.readUnsignedShort()
        if (count.toLong() !in 1..((input.length() - 78) / 8)) throw InvalidBookMetadata()
        return count
    }
}
