package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.BookCover
import dev.twotechy.inknest.ui.components.quietClick
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkButtonTokens
import dev.twotechy.inknest.ui.theme.InkHomeTokens
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkReadingTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun ContinueReadingCard(book: BookUiModel, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().quietClick(onClick),
        shape = RoundedCornerShape(InkRadius.large),
        border = BorderStroke(InkBorder.default, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(InkLayoutTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fontScale = LocalDensity.current.fontScale
                val coverWidth = (maxWidth * 0.23f).coerceIn(
                    InkReadingTokens.coverMinWidth,
                    InkReadingTokens.coverMaxWidth,
                )
                // Reserve readable text width, including the user's accessibility scale.
                val stacked =
                    maxWidth <
                        coverWidth + InkSpacing.lg + InkReadingTokens.minDetailsWidth * fontScale
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
                        BookCover(book.coverUri, Modifier.width(coverWidth).aspectRatio(0.7f))
                        CurrentBookDetails(book)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BookCover(book.coverUri, Modifier.width(coverWidth).aspectRatio(0.7f))
                        CurrentBookDetails(book, Modifier.weight(1f))
                    }
                }
            }
            book.progress?.let { ProgressTrack(it) }
        }
    }
}

@Composable
private fun CurrentBookDetails(book: BookUiModel, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        Text(
            book.title,
            style = InkTextStyles.cardTitle,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            book.author ?: stringResource(R.string.reading_unknown_author),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProgressLabel(book)
        ContinueButton()
    }
}

// The entire card owns the opening action, including the cover and this visual affordance.
@Composable
private fun ContinueButton() {
    Surface(
        modifier = Modifier.widthIn(min = InkButtonTokens.minWidth)
            .heightIn(min = InkHomeTokens.continueMinHeight),
        shape = RoundedCornerShape(InkRadius.medium),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            Modifier.padding(
                horizontal = InkButtonTokens.horizontalPadding,
                vertical = InkSpacing.xs,
            ),
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_play),
                null,
                Modifier.size(InkHomeTokens.continueIconSize),
            )
            Text(stringResource(R.string.home_continue), style = InkTextStyles.labelLarge)
        }
    }
}

@Composable
private fun ProgressLabel(book: BookUiModel) {
    val percent = book.progress?.let {
        stringResource(R.string.reading_progress_percent, it)
    }
    val pages = if (book.currentPage != null && book.totalPages != null) {
        stringResource(R.string.home_reading_pages, book.currentPage, book.totalPages)
    } else {
        null
    }
    Text(
        listOfNotNull(percent, pages).joinToString(" · ").ifEmpty {
            stringResource(R.string.reading_progress_unknown)
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProgressTrack(progress: Int) {
    val ink = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(R.string.reading_progress_percent, progress)
    Canvas(
        Modifier.fillMaxWidth().height(InkReadingTokens.progressHeight).semantics {
            contentDescription =
                description
        },
    ) {
        drawRect(track)
        drawRect(ink, size = Size(size.width * progress.coerceIn(0, 100) / 100f, size.height))
    }
}
