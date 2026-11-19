package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkTextStyles

@Composable
internal fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    isDetail: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.minHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (onBack != null) Spacer(Modifier.width(InkHeaderTokens.iconSize))
            Text(
                title,
                style = if (isDetail) InkTextStyles.subPageTitle else InkTextStyles.pageTitle,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        // Expand Back over passive title space while keeping its visible slot compact.
        onBack?.let { back ->
            Box(Modifier.align(Alignment.CenterStart)) {
                HeaderIconButton(
                    R.drawable.ic_back,
                    stringResource(R.string.back),
                    placement = HeaderIconPlacement.LeadingBack,
                    onClick = back,
                )
            }
        }
    }
}

@Composable
internal fun HeaderIconButton(
    icon: Int,
    description: String,
    placement: HeaderIconPlacement = HeaderIconPlacement.Trailing,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(InkHeaderTokens.touchTarget).quietClick(onClick),
        contentAlignment = when (placement) {
            HeaderIconPlacement.Trailing -> Alignment.CenterEnd
            HeaderIconPlacement.Centered -> Alignment.Center
            HeaderIconPlacement.LeadingBack -> Alignment.CenterStart
        },
    ) {
        Icon(
            painterResource(icon),
            description,
            Modifier.offset(
                x = if (placement ==
                    HeaderIconPlacement.LeadingBack
                ) {
                    -InkHeaderTokens.backIconStartInset
                } else {
                    0.dp
                },
            ).size(InkHeaderTokens.iconSize),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
