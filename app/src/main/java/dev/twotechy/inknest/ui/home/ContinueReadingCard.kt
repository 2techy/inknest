package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.ui.components.quietClick

@Composable
internal fun ContinueReadingCard(book: BookUiModel, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fontScale = LocalDensity.current.fontScale
                val coverWidth = (maxWidth * 0.23f).coerceIn(64.dp, 112.dp)
                // Reserve readable text width, including the user's accessibility scale.
                val stacked = maxWidth < coverWidth + 16.dp + 150.dp * fontScale
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BookCover(book, Modifier.width(coverWidth).aspectRatio(0.7f))
                        CurrentBookDetails(book)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BookCover(book, Modifier.width(coverWidth).aspectRatio(0.7f))
                        CurrentBookDetails(book, Modifier.weight(1f))
                    }
                }
            }
            book.progress?.let { ProgressTrack(it) }
            Box(
                Modifier.fillMaxWidth().background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(4.dp),
                )
                    .quietClick(onClick).padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Continue reading  →",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun CurrentBookDetails(book: BookUiModel, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(book.title, style = MaterialTheme.typography.titleLarge)
        Text(
            book.author ?: "Unknown author",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProgressLabel(book.progress)
    }
}

@Composable
private fun BookCover(book: BookUiModel, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
            .border(1.25.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
            .padding(8.dp).semantics {
                contentDescription =
                    if (book.coverLabel == null) "No cover" else "Sample book cover"
            },
        contentAlignment = Alignment.Center,
    ) {
        // Cover art is decorative: drawing it avoids scaled text overflowing tiny thumbnails.
        Canvas(Modifier.fillMaxSize()) {
            if (book.coverLabel != null) {
                drawLine(
                    ink,
                    Offset(size.width * 0.15f, 0f),
                    Offset(size.width * 0.15f, size.height),
                    1.dp.toPx(),
                )
                drawCircle(
                    ink,
                    size.width * 0.22f,
                    Offset(size.width * 0.6f, size.height * 0.4f),
                    style = Stroke(1.dp.toPx()),
                )
                drawLine(
                    ink,
                    Offset(size.width * 0.35f, size.height * 0.75f),
                    Offset(size.width * 0.85f, size.height * 0.75f),
                    1.dp.toPx(),
                )
            } else {
                drawLine(
                    ink,
                    Offset(size.width * 0.3f, size.height * 0.5f),
                    Offset(size.width * 0.7f, size.height * 0.5f),
                    1.dp.toPx(),
                )
            }
        }
    }
}

@Composable
private fun ProgressLabel(progress: Int?) {
    Text(
        progress?.let {
            "$it% read"
        } ?: "Progress unavailable",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProgressTrack(progress: Int) {
    val ink = MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        Modifier.fillMaxWidth().height(4.dp).semantics {
            contentDescription =
                "$progress percent read"
        },
    ) {
        drawRect(track)
        drawRect(ink, size = Size(size.width * progress.coerceIn(0, 100) / 100f, size.height))
    }
}
