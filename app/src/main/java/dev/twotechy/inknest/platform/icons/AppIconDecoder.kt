package dev.twotechy.inknest.platform.icons

import androidx.compose.ui.graphics.ImageBitmap

internal fun interface AppIconDecoder {
    fun load(request: AppIconRequest): ImageBitmap?
}
