package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkButtonTokens
import dev.twotechy.inknest.ui.theme.InkRadius

@Composable
internal fun ActionButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(InkRadius.medium)
    val ink = if (enabled) colors.onSurface else colors.outline
    Box(
        modifier.widthIn(min = InkButtonTokens.minWidth).heightIn(min = InkButtonTokens.minHeight)
            .background(if (primary && enabled) ink else colors.surface, shape)
            .border(
                InkBorder.default,
                if (primary && enabled) ink else colors.outlineVariant,
                shape,
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ).padding(
                horizontal = InkButtonTokens.horizontalPadding,
                vertical = InkButtonTokens.verticalPadding,
            ),

        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = if (primary && enabled) colors.surface else ink,
            textAlign = TextAlign.Center,
        )
    }
}
