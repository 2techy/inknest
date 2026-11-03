package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import dev.twotechy.inknest.platform.AppIconLoader

@Composable
internal fun AppIcon(
    componentId: String,
    label: String,
    iconVersion: Long,
    loader: AppIconLoader?,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales.toLanguageTags()
    var icon by remember(componentId, iconVersion, locale) { mutableStateOf<ImageBitmap?>(null) }
    val grayscale =
        remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    LaunchedEffect(componentId, iconVersion, locale, loader) {
        icon = loader?.loadIcon(componentId, iconVersion)
    }
    val current = icon
    if (current == null) {
        Text(label.take(1), fontFamily = FontFamily.Serif, fontSize = 24.sp)
    } else {
        Image(current, contentDescription = null, modifier = modifier, colorFilter = grayscale)
    }
}
