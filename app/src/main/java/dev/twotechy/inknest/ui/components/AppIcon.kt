package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalConfiguration
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppIconRequest
import dev.twotechy.inknest.ui.theme.LocalDisplayMode

@Composable
internal fun AppIcon(
    componentId: String,
    label: String,
    iconVersion: Long,
    loader: AppIconLoader?,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val request = AppIconRequest(
        componentId,
        iconVersion,
        configuration.locales.toLanguageTags(),
        configuration.densityDpi,
    )
    val icon = remember(loader, request) { mutableStateOf(loader?.peekIcon(request)) }
    val grayscale =
        remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    LaunchedEffect(loader, request) {
        if (icon.value == null) icon.value = loader?.loadIcon(request)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val current = icon.value
        if (current == null) {
            Text(
                label.take(1),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
        } else {
            Image(
                current,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (LocalDisplayMode.current == DisplayMode.EInk) grayscale else null,
            )
        }
    }
}
