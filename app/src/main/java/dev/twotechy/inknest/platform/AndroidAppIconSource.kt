package dev.twotechy.inknest.platform

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.LocaleList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CancellationException

internal class AndroidAppIconSource(private val context: Context) : AppIconSource {
    override fun load(request: AppIconRequest): ImageBitmap? {
        val parts = request.componentId.split("/", limit = 2)
        if (parts.size != 2) return null
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(request.locale))
            densityDpi = request.densityDpi
        }
        val packageManager = context.createConfigurationContext(configuration).packageManager
        return try {
            val component = ComponentName(parts[0], parts[1])
            val activityInfo = packageManager.getActivityInfo(component, 0)
            drawableToBitmap(activityInfo.loadIcon(packageManager)).asImageBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Package/resource changes can invalidate an icon while the catalog is refreshing.
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = createBitmap(128, 128)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
