package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun SectionAction(label: String, onClick: () -> Unit) {
    Row(
        Modifier.quietClick(onClick).sizeIn(
            minWidth = InkHeaderTokens.touchTarget,
            minHeight = InkHeaderTokens.touchTarget,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            null,
            Modifier.size(InkHeaderTokens.iconSize),
        )
    }
}
