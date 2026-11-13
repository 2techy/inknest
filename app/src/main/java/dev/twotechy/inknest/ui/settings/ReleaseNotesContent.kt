package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun ReleaseNotesContent(document: ReleaseNotesDocument, openLink: (String) -> Unit) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.md)) {
            document.blocks.forEach { block -> ReleaseNote(block, openLink) }
        }
    }
}

@Composable
private fun ReleaseNote(block: ReleaseNoteBlock, openLink: (String) -> Unit) {
    if (block.style == ReleaseNoteStyle.Rule) {
        HorizontalDivider(
            thickness = InkBorder.thin,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        return
    }
    val typography = MaterialTheme.typography
    val textStyle = when (block.style) {
        ReleaseNoteStyle.Heading -> typography.titleMedium
        ReleaseNoteStyle.Code -> typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        ReleaseNoteStyle.Quote -> typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
        else -> typography.bodyMedium
    }
    Row(
        Modifier.padding(start = InkSpacing.lg * block.depth),
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        block.marker?.let { Text(it, style = typography.bodyMedium) }
        val decoration = when (block.style) {
            ReleaseNoteStyle.Heading -> Modifier.semantics { heading() }

            ReleaseNoteStyle.Code -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(InkSpacing.sm)

            else -> Modifier
        }
        val text = remember(block.text, openLink) {
            bindReleaseNoteLinks(block.text, openLink)
        }
        Text(text, Modifier.weight(1f).then(decoration), style = textStyle)
    }
}
