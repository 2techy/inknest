package dev.twotechy.inknest.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AppIconLoader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private data class IconKey(val componentId: String, val version: Long)

    // Bound decoded image dimensions as well as entry count (roughly 6 MiB maximum).
    private val cache = LruCache<IconKey, ImageBitmap>(100)

    suspend fun loadIcon(componentId: String, version: Long = 0): ImageBitmap? =
        withContext(ioDispatcher) {
            val key = IconKey(componentId, version)
            cache[key]?.let { return@withContext it }

            val parts = componentId.split("/", limit = 2)
            if (parts.size != 2) return@withContext null

            val packageName = parts[0]
            val className = parts[1]
            val packageManager = context.packageManager

            try {
                val componentName = ComponentName(packageName, className)
                val activityInfo = packageManager.getActivityInfo(componentName, 0)
                val drawable = activityInfo.loadIcon(packageManager)
                val bitmap = drawableToBitmap(drawable)
                val imageBitmap = bitmap.asImageBitmap()
                cache.put(key, imageBitmap)
                imageBitmap
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (_: Exception) {
                null
            }
        }

    fun clear() {
        cache.evictAll()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
