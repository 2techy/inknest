package dev.twotechy.inknest.data.source

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.time.Clock
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal interface UpdateHttpTransport {
    suspend fun text(url: String, limit: Int): String
    suspend fun download(url: String, target: File, size: Long, progress: (Int) -> Unit)
}

internal class HttpsUpdateTransport(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
) : UpdateHttpTransport {
    override suspend fun text(url: String, limit: Int): String = request(url) { connection ->
        val output = ByteArrayOutputStream()
        connection.inputStream.use { input ->
            copy(input, limit.toLong()) { bytes, count ->
                output.write(bytes, 0, count)
            }
        }
        output.toString(Charsets.UTF_8.name())
    }

    override suspend fun download(url: String, target: File, size: Long, progress: (Int) -> Unit) {
        if (size !in 1..MAX_UPDATE_APK_BYTES) throw IOException("Invalid APK size")
        request(url) { connection ->
            val length = connection.contentLengthLong
            if (length >= 0 && length != size) throw IOException("Unexpected content length")
            var total = 0L
            val reporter = UpdateDownloadProgress(size, progress)
            target.outputStream().use { output ->
                connection.inputStream.use { input ->
                    copy(input, size) { bytes, count ->
                        output.write(bytes, 0, count)
                        total += count
                        reporter.report(total)
                    }
                }
            }
            if (total != size) throw IOException("Incomplete APK")
            progress(100)
        }
    }

    private suspend fun copy(input: InputStream, limit: Long, consume: (ByteArray, Int) -> Unit) {
        val buffer = ByteArray(16_384)
        var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) throw IOException("Response exceeds size limit")
            consume(buffer, count)
        }
    }

    private suspend fun <T> request(url: String, read: suspend (HttpsURLConnection) -> T): T =
        withContext(dispatcher) {
            var location = URL(url)
            repeat(6) {
                validateLocation(location)
                val connection = location.openConnection() as HttpsURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("User-Agent", "InkNest-Updater")
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                val result = connected(connection) {
                    when (val status = connection.responseCode) {
                        200 -> return@connected read(connection)

                        301, 302, 303, 307, 308 -> {
                            val next = connection.getHeaderField("Location")
                                ?: throw IOException("Missing redirect location")
                            location = URL(location, next)
                            null
                        }

                        403, 429 -> throw UpdateRateLimitException(retryAt(connection))

                        else -> throw IOException("GitHub HTTP $status")
                    }
                }
                if (result != null) return@withContext result
            }
            throw IOException("Too many redirects")
        }

    private suspend fun <T> connected(connection: HttpsURLConnection, read: suspend () -> T): T =
        coroutineScope {
            // Closing the socket on cancellation also unblocks a pending platform read.
            val closer = launch(dispatcher, start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    connection.disconnect()
                }
            }
            try {
                read()
            } finally {
                connection.disconnect()
                closer.cancelAndJoin()
            }
        }

    private fun retryAt(connection: HttpsURLConnection): Long {
        val now = clock.millis()
        val reset = connection.getHeaderField("X-RateLimit-Reset")?.toLongOrNull()
            ?.takeIf { it in 1..Long.MAX_VALUE / 1000 }?.times(1000)
        val retry = connection.getHeaderField("Retry-After")?.toLongOrNull()
            ?.coerceIn(1, 86_400)?.let { now + it * 1000 }
        return maxOf(now + 60_000, reset ?: 0, retry ?: now + 3_600_000)
    }

    private fun validateLocation(url: URL) {
        val hosts = setOf(
            "api.github.com",
            "github.com",
            "release-assets.githubusercontent.com",
            "objects.githubusercontent.com",
        )
        if (url.protocol != "https" || url.port !in listOf(-1, 443)) {
            throw IOException("Insecure update location")
        }
        if (url.host !in hosts ||
            url.userInfo != null
        ) {
            throw IOException("Untrusted update location")
        }
    }
}

private class UpdateDownloadProgress(private val size: Long, private val notify: (Int) -> Unit) {
    private var lastProgress = -1
    private var lastTick = 0L

    fun report(total: Long) {
        val percent = (total * 100 / size).toInt()
        val tick = System.nanoTime()
        if (percent != lastProgress && tick - lastTick >= 1_000_000_000L) {
            notify(percent)
            lastProgress = percent
            lastTick = tick
        }
    }
}
