package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.ui.components.BlockContentGestures
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun LibraryBookDialog(
    book: ReadingItem,
    direct: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    BlockContentGestures()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(book.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
            ) {
                book.author?.let { Text(it) }
                Text(
                    book.id.path.removePrefix("primary:"),
                    style = MaterialTheme.typography.bodySmall,
                )
                book.progress?.let { Text(stringResource(R.string.reading_progress_percent, it)) }
                if (!direct) Text(stringResource(R.string.library_choose_in_reader))
            }
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Text(
                    stringResource(
                        if (direct) R.string.reading_continue else R.string.reading_open_koreader,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
