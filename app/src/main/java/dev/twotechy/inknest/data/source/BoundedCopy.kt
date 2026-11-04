package dev.twotechy.inknest.data.source

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun copyBounded(input: InputStream, output: OutputStream, limit: Long) {
    var total = 0L
    val buffer = ByteArray(8192)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) return
        total += count
        if (total > limit) throw SourceLimitExceeded()
        output.write(buffer, 0, count)
    }
}
internal class SourceLimitExceeded : IOException()
