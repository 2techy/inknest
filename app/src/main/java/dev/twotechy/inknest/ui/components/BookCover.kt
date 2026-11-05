package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.platform.BookCoverRequest
import dev.twotechy.inknest.platform.BookCoverSize
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.LocalDisplayMode

@Composable
internal fun BookCover(uri: String?, modifier: Modifier = Modifier) {
    val loader = LocalBookCoverLoader.current
    val grayscale =
        remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    BoxWithConstraints(
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
        val size = BookCoverSize.forBounds(constraints.maxWidth, constraints.maxHeight)
        val request = uri?.let { BookCoverRequest(it, size) }
        val image = remember(loader, request) {
            mutableStateOf(request?.let { loader?.peekCover(it) })
        }
        LaunchedEffect(loader, request) {
            if (image.value == null && request != null) image.value = loader?.loadCover(request)
        }
        val current = image.value
        if (current != null) {
            Image(
                current,
                null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (LocalDisplayMode.current == DisplayMode.EInk) grayscale else null,
            )
        } else {
            BookCoverPlaceholder(Modifier.fillMaxSize())
        }
    }
}
