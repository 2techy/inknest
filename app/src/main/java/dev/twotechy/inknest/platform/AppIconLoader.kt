package dev.twotechy.inknest.platform

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class AppIconRequest(
    val componentId: String,
    val version: Long,
    val locale: String,
    val densityDpi: Int,
)

internal fun interface AppIconSource {
    fun load(request: AppIconRequest): ImageBitmap?
}

internal class AppIconLoader(
    private val source: AppIconSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // Keep the existing bound: 100 icons, each 128 x 128 pixels (roughly 6 MiB).
    private val cache = LruCache<AppIconRequest, ImageBitmap>(100)

    // Only a RAM lookup: safe to use when initializing a composable's state.
    fun peekIcon(request: AppIconRequest): ImageBitmap? = cache[request]

    suspend fun loadIcon(request: AppIconRequest): ImageBitmap? {
        peekIcon(request)?.let { return it }
        return withContext(ioDispatcher) {
            peekIcon(request)?.let { return@withContext it }
            val image = source.load(request)
            currentCoroutineContext().ensureActive()
            image?.also { cache.put(request, it) }
        }
    }

    fun clear() = cache.evictAll()
}
