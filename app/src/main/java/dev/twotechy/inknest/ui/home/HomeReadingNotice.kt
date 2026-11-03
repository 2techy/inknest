package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.components.quietClick
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkHomeTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSpacing

// Reading setup/status is secondary to the usable launcher content on Home.
@Composable
internal fun HomeReadingNotice(
    title: String,
    detail: String,
    icon: Int = R.drawable.ic_nav_library_outline,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = InkHomeTokens.noticeMinHeight)
            .border(
                InkBorder.default,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(InkRadius.medium),
            )
            .then(if (onClick != null) Modifier.quietClick(onClick) else Modifier)
            .padding(InkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
    ) {
        Icon(painterResource(icon), null, Modifier.size(InkHeaderTokens.iconSize))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                null,
                Modifier.size(InkHeaderTokens.iconSize),
            )
        }
    }
}
