package dev.twotechy.inknest.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkReadingTokens
import dev.twotechy.inknest.ui.theme.LocalDisplayMode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun BookCover(uri: String?, modifier: Modifier = Modifier) {
    var image by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    val grayscale =
        remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    LaunchedEffect(uri) {
        image = withContext(Dispatchers.IO) {
            uri?.let { value ->
                val parsed = Uri.parse(value)
                if (parsed.scheme != "file") return@let null
                val file = parsed.path?.let(::File) ?: return@let null
                // Only thumbnails written by our bounded decoder are passed to this component.
                BitmapFactory.decodeFile(file.path)?.asImageBitmap()
            }
        }
    }
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier.background(
            MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(InkRadius.small),
        )
            .border(
                InkBorder.default,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(InkRadius.small),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val current = image
        if (current != null) {
            Image(
                current,
                null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (LocalDisplayMode.current == DisplayMode.EInk) grayscale else null,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    ink,
                    Offset(size.width * 0.3f, size.height * 0.5f),
                    Offset(size.width * 0.7f, size.height * 0.5f),
                    InkReadingTokens.artworkStroke.toPx(),
                )
            }
        }
    }
}
