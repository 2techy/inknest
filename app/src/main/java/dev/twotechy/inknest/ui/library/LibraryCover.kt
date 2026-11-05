package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.ui.components.BookCover
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkSpacing
import java.util.Locale

@Composable
internal fun LibraryCover(book: ReadingItem, modifier: Modifier) {
    if (book.coverUri != null) {
        BookCover(book.coverUri, modifier)
    } else {
        val ink = MaterialTheme.colorScheme.outlineVariant
        Box(modifier.border(InkBorder.default, ink).clearAndSetSemantics {}) {
            Canvas(Modifier.fillMaxSize()) {
                val x = InkSpacing.sm.toPx()
                drawLine(ink, Offset(x, 0f), Offset(x, size.height), InkBorder.thin.toPx())
            }
            Column(
                Modifier.fillMaxSize().padding(
                    start = InkSpacing.lg,
                    end = InkSpacing.sm,
                    top = InkSpacing.md,
                    bottom = InkSpacing.md,
                ),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    book.id.path.substringAfterLast('.', "BOOK").uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    book.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
