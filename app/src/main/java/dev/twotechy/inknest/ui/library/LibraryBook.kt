package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkLibraryTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun LibraryGridBook(book: ReadingItem, onClick: () -> Unit, onDetails: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        }.bookActions(onClick, onDetails),
        verticalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LibraryCover(
                book,
                Modifier.widthIn(
                    max = InkLibraryTokens.maxCoverWidth,
                ).fillMaxWidth().aspectRatio(0.7f),
            )
        }
        LibraryBookDetails(book)
    }
}

@Composable
internal fun LibraryListBook(book: ReadingItem, onClick: () -> Unit, onDetails: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            }.bookActions(onClick, onDetails)
                .padding(vertical = InkSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryCover(
                book,
                Modifier.width(InkLibraryTokens.listCoverWidth).aspectRatio(0.7f),
            )
            LibraryBookDetails(book, Modifier.weight(1f), compact = false)
        }
        HorizontalDivider(
            thickness = InkBorder.thin,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun LibraryBookDetails(
    book: ReadingItem,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        Text(
            book.title,
            style = if (compact) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.titleSmall
            },
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        book.author?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        book.progress?.let {
            Text(
                stringResource(R.string.reading_progress_percent, it),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (book.status == ReadingStatus.Finished || book.status == ReadingStatus.Abandoned) {
            Text(
                stringResource(
                    if (book.status == ReadingStatus.Finished) {
                        R.string.library_filter_finished
                    } else {
                        R.string.library_status_abandoned
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        book.availability.label()?.let {
            Text(stringResource(it), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun BookAvailability.label(): Int? = when (this) {
    BookAvailability.Available -> null
    BookAvailability.Missing -> R.string.library_file_missing
    BookAvailability.PermissionLost -> R.string.library_permission_lost
    BookAvailability.Unresolved -> R.string.library_unresolved
}

@Composable
private fun Modifier.bookActions(onClick: () -> Unit, onDetails: () -> Unit): Modifier =
    combinedClickable(
        interactionSource = null,
        indication = null,
        role = Role.Button,
        onLongClickLabel = stringResource(R.string.library_details),
        onLongClick = onDetails,
        onClick = onClick,
    )
