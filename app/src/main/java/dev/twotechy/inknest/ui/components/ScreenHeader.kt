package dev.twotechy.inknest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    Row(
        Modifier.fillMaxWidth().heightIn(min = InkHeaderTokens.minHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (onBack != null) {
            HeaderButton(
                R.drawable.ic_back,
                stringResource(R.string.back),
                onBack,
                alignToContent = true,
            )
        }
        Text(
            title,
            style = if (isDetail) InkTextStyles.subPageTitle else InkTextStyles.pageTitle,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
internal fun HeaderIconButton(icon: Int, description: String, onClick: () -> Unit) {
    HeaderButton(icon, description, onClick)
}

@Composable
private fun HeaderButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    alignToContent: Boolean = false,
) {
    val width = if (alignToContent) {
        InkHeaderTokens.iconSize
    } else {
        InkHeaderTokens.touchTarget
    }
    Box(
        Modifier.size(width = width, height = InkHeaderTokens.touchTarget).quietClick(onClick),
        contentAlignment = if (alignToContent) Alignment.CenterStart else Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            description,
            (
                if (alignToContent) {
                    Modifier.offset(x = -InkHeaderTokens.backIconStartInset)
                } else {
                    Modifier
                }
                ).size(InkHeaderTokens.iconSize),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
