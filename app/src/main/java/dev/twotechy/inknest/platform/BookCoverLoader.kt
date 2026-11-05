package dev.twotechy.inknest.platform

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class DecodedBookCover(val image: ImageBitmap, val byteCount: Int)

internal fun interface BookCoverSource {
    suspend fun load(request: BookCoverRequest): DecodedBookCover?
}

internal class BookCoverLoader(
    private val source: BookCoverSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val cache = object : LruCache<BookCoverRequest, DecodedBookCover>(4 * 1024 * 1024) {
        override fun sizeOf(key: BookCoverRequest, value: DecodedBookCover): Int = value.byteCount
    }
    private val decode = Mutex()
    private val lock = Any()
    private var generation = 0L

    fun peekCover(request: BookCoverRequest): ImageBitmap? = synchronized(lock) {
        cache[request]?.image
    }

    suspend fun loadCover(request: BookCoverRequest): ImageBitmap? {
        peekCover(request)?.let { return it }
        return withContext(ioDispatcher) {
            decode.withLock {
                peekCover(request)?.let { return@withLock it }
                val epoch = synchronized(lock) { generation }
                val result = source.load(request) ?: return@withLock null
                currentCoroutineContext().ensureActive()
                synchronized(lock) {
                    if (epoch != generation) return@synchronized null
                    cache.put(request, result)
                    result.image
                }
            }
        }
    }

    fun clear() = synchronized(lock) {
        generation++
        cache.evictAll()
    }
}
