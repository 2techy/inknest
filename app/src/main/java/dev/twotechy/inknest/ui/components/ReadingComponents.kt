package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(title, style = InkTextStyles.sectionTitle)
    }
}

@Composable
internal fun StatePanel(
    title: String,
    detail: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().border(
            InkBorder.default,
            MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(InkRadius.large),
        )
            .padding(InkLayoutTokens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            detail,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            ActionButton(action, onClick = onAction)
        }
    }
}

internal fun Modifier.quietClick(onClick: () -> Unit): Modifier = clickable(
    interactionSource = null,
    indication = null,
    role = Role.Button,
    onClick = onClick,
)
