package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun ReadingStatsRow(
    title: String,
    amount: StatisticsAmount,
    alignChevrons: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val action = if (onClick == null) Modifier else Modifier.quietClick(onClick)
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.touchTarget)
                .then(action).padding(vertical = InkSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(2f), style = InkTextStyles.bodyMedium)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    formatStatsAmount(amount, StatisticsUnit.Time),
                    style = InkTextStyles.labelLarge,
                    textAlign = TextAlign.End,
                )
                Text(
                    formatStatsAmount(amount, StatisticsUnit.Pages),
                    style = InkTextStyles.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
            if (onClick != null) {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    null,
                    Modifier.size(InkHeaderTokens.iconSize).rotate(if (rtl) 180f else 0f),
                )
            } else if (alignChevrons) {
                Spacer(Modifier.size(InkHeaderTokens.iconSize))
            }
        }
        HorizontalDivider()
    }
}
