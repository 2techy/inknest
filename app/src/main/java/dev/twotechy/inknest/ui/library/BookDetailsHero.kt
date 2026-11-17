package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.ui.components.BookCover
import dev.twotechy.inknest.ui.components.currentUiLocale
import dev.twotechy.inknest.ui.theme.InkButtonTokens
import dev.twotechy.inknest.ui.theme.InkHomeTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkReadingTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun BookDetailsHero(book: ReadingItem, zone: ZoneId, open: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.md)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val hasProgress =
                book.progress != null || (book.currentPage != null && book.totalPages != null)
            val cover = if (!hasProgress) {
                InkReadingTokens.coverMinWidth
            } else {
                (maxWidth * 0.23f).coerceIn(
                    InkReadingTokens.coverMinWidth,
                    InkReadingTokens.coverMaxWidth,
                )
            }
            val minText = InkReadingTokens.minDetailsWidth * LocalDensity.current.fontScale
            val stacked = maxWidth < cover + InkSpacing.lg + minText
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
                    BookCover(book.coverUri, Modifier.width(cover).aspectRatio(0.7f))
                    BookHeroText(book, zone)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(InkSpacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    BookCover(book.coverUri, Modifier.width(cover).aspectRatio(0.7f))
                    BookHeroText(book, zone, Modifier.weight(1f))
                }
            }
        }
        BookReadButton(book, open)
    }
}

@Composable
private fun BookHeroText(book: ReadingItem, zone: ZoneId, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(InkSpacing.sm)) {
        Text(book.title, style = InkTextStyles.cardTitle)
        book.author?.let {
            Text(
                it,
                style = InkTextStyles.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val status = when (book.status) {
            ReadingStatus.Finished -> stringResource(R.string.library_filter_finished)
            ReadingStatus.Abandoned -> stringResource(R.string.library_status_abandoned)
            else -> null
        }
        val progress = book.progress?.let {
            stringResource(R.string.library_cover_progress_percent, it)
        }
        val pages = if (book.currentPage != null && book.totalPages != null) {
            stringResource(R.string.home_reading_pages, book.currentPage, book.totalPages)
        } else {
            null
        }
        val label = listOfNotNull(status ?: progress, pages).joinToString(" · ")
        if (label.isNotEmpty()) Text(label, style = InkTextStyles.labelLarge)
        book.progress?.let { BookDetailsProgress(it) }
        book.lastReadAt?.let { timestamp ->
            val formatted = DateTimeFormatter.ofLocalizedDateTime(
                FormatStyle.MEDIUM,
                FormatStyle.SHORT,
            )
                .withLocale(currentUiLocale()).format(Instant.ofEpochMilli(timestamp).atZone(zone))
            Text(
                stringResource(R.string.book_details_last_read_value, formatted),
                style = InkTextStyles.bodySmall,
            )
        }
        if (book.availability != BookAvailability.Available) {
            Text(stringResource(book.availability.detailsLabel()), style = InkTextStyles.bodySmall)
        }
    }
}

@Composable
private fun BookDetailsProgress(progress: Int) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(
        Modifier.fillMaxWidth().height(InkReadingTokens.progressHeight).then(
            Modifier.semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    progress.coerceIn(0, 100) / 100f,
                    0f..1f,
                )
            },
        ),
    ) {
        drawRect(track)
        drawRect(ink, size = Size(size.width * progress.coerceIn(0, 100) / 100f, size.height))
    }
}

@Composable
private fun BookReadButton(book: ReadingItem, open: () -> Unit) {
    val label = when {
        book.status == ReadingStatus.Finished -> R.string.book_details_read_again
        book.status == ReadingStatus.Reading || book.progress != null -> R.string.home_continue
        else -> R.string.reading_open_book
    }
    val enabled = book.availability == BookAvailability.Available
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().heightIn(min = InkButtonTokens.minHeight)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = null,
                indication = null,
                onClick = open,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth().heightIn(min = InkHomeTokens.continueMinHeight),
            shape = RoundedCornerShape(InkRadius.medium),
            color = if (enabled) colors.onSurface else colors.surfaceVariant,
            contentColor = if (enabled) colors.surface else colors.onSurfaceVariant,
        ) {
            Row(
                Modifier.padding(
                    horizontal = InkButtonTokens.horizontalPadding,
                    vertical = InkSpacing.xs,
                ),
                horizontalArrangement = Arrangement.spacedBy(
                    InkSpacing.sm,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_play),
                    null,
                    Modifier.size(InkHomeTokens.continueIconSize),
                )
                Text(stringResource(label), style = InkTextStyles.labelLarge)
            }
        }
    }
}

internal fun BookAvailability.detailsLabel() = when (this) {
    BookAvailability.Available -> R.string.book_details_available
    BookAvailability.Missing -> R.string.library_file_missing
    BookAvailability.PermissionLost -> R.string.library_permission_lost
    BookAvailability.Unresolved -> R.string.library_unresolved
}
