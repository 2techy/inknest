package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun ReadingStatsPager(label: String, onPrevious: (() -> Unit)?, onNext: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PageButton(stringResource(R.string.reading_stats_previous_page), true, onPrevious)
        Text(
            label,
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = InkTextStyles.bodySmall,
        )
        PageButton(stringResource(R.string.reading_stats_next_page), false, onNext)
    }
}

@Composable
private fun PageButton(label: String, backwards: Boolean, onClick: (() -> Unit)?) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val colors = MaterialTheme.colorScheme
    val enabled = onClick != null
    Box(
        Modifier.size(InkHeaderTokens.touchTarget).clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = null,
            indication = null,
            onClick = { onClick?.invoke() },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            label,
            Modifier.size(InkHeaderTokens.iconSize).rotate(if (backwards != rtl) 180f else 0f),
            tint = if (enabled) colors.onSurface else colors.outline,
        )
    }
}
