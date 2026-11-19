package dev.twotechy.inknest.platform.covers

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class BookCoverLoader(
    private val source: BookCoverDecoder,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    maxBytes: Int = (Runtime.getRuntime().maxMemory() / 8).coerceIn(
        1,
        Int.MAX_VALUE.toLong(),
    ).toInt(),
    parentScope: CoroutineScope? = null,
) {
    private val scope =
        CoroutineScope(SupervisorJob(parentScope?.coroutineContext?.get(Job)) + ioDispatcher)
    private val cache = object : LruCache<BookCoverRequest, DecodedBookCover>(maxBytes) {
        override fun sizeOf(key: BookCoverRequest, value: DecodedBookCover): Int = value.byteCount
    }
    private val decode = Semaphore(2)
    private val inFlight = mutableMapOf<BookCoverRequest, Deferred<ImageBitmap?>>()
    private val lock = Any()
    private var generation = 0L

    fun peekCover(request: BookCoverRequest): ImageBitmap? = synchronized(lock) {
        cache[request]?.image
    }

    suspend fun loadCover(request: BookCoverRequest): ImageBitmap? {
        peekCover(request)?.let { return it }
        val pending = synchronized(lock) {
            cache[request]?.let { return it.image }
            inFlight.getOrPut(request) { startDecode(request, generation) }
        }
        pending.start()
        return pending.await()
    }

    private fun startDecode(request: BookCoverRequest, epoch: Long): Deferred<ImageBitmap?> {
        val pending = scope.async(start = CoroutineStart.LAZY) {
            decode.withPermit {
                if (synchronized(lock) { epoch != generation }) return@withPermit null
                val result = source.load(request) ?: return@withPermit null
                currentCoroutineContext().ensureActive()
                synchronized(lock) {
                    if (epoch != generation) return@synchronized null
                    cache.put(request, result)
                    result.image
                }
            }
        }
        pending.invokeOnCompletion {
            synchronized(lock) {
                if (inFlight[request] === pending) inFlight.remove(request)
            }
        }
        return pending
    }

    fun clear() = synchronized(lock) {
        generation++
        cache.evictAll()
        inFlight.clear()
    }
}
