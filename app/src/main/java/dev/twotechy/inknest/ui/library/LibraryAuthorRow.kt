package dev.twotechy.inknest.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun LibraryAuthorRow(author: LibraryAuthor, click: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(
            min = InkHeaderTokens.touchTarget,
        ).clickable(
            role = Role.Button,
            interactionSource = remember {
                MutableInteractionSource()
            },
            indication = null,
            onClick = click,
        ).padding(vertical = InkSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        Text(
            author.name ?: stringResource(R.string.library_unknown_author),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            pluralStringResource(R.plurals.library_author_count, author.count, author.count),
            style = MaterialTheme.typography.bodyMedium,
        )
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(InkHeaderTokens.iconSize),
        )
    }
}
