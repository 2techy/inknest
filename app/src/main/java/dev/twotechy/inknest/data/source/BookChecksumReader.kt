package dev.twotechy.inknest.data.source

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

// KOReader util.partialMD5: 1 KiB at 0, 1 KiB, 4 KiB, ... 1 GiB (at most 12 KiB read).
internal suspend fun partialBookMd5(channel: SeekableByteChannel): String {
    val digest = MessageDigest.getInstance("MD5")
    val buffer = ByteBuffer.allocate(1024)
    val offsets = listOf(0L) + (0..10).map { 1024L shl (2 * it) }
    for (offset in offsets) {
        currentCoroutineContext().ensureActive()
        channel.position(offset)
        readSample(channel, buffer)
        if (buffer.position() == 0) break
        digest.update(buffer.array(), 0, buffer.position())
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
}

private suspend fun readSample(channel: SeekableByteChannel, buffer: ByteBuffer) {
    buffer.clear()
    while (buffer.hasRemaining()) {
        currentCoroutineContext().ensureActive()
        val count = channel.read(buffer)
        if (count < 0) break
        if (count == 0) throw IOException("Provider returned no bytes")
    }
}
