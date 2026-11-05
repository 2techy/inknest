package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.twotechy.inknest.ui.theme.InkChoiceTokens
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkRadius

// The visible tab stays compact; its full row height remains available for touch.
@Composable
internal fun ChoiceTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.sizeIn(
            minWidth = InkHeaderTokens.touchTarget,
            minHeight = InkHeaderTokens.touchTarget,
        )
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth().heightIn(min = InkChoiceTokens.minHeight),
            shape = RoundedCornerShape(InkRadius.small),
            color = if (selected) colors.onSurface else colors.surface,
            contentColor = if (selected) colors.surface else colors.onSurface,
        ) {
            Text(
                label,
                Modifier.padding(
                    horizontal = InkChoiceTokens.horizontalPadding,
                    vertical = InkChoiceTokens.verticalPadding,
                ),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
