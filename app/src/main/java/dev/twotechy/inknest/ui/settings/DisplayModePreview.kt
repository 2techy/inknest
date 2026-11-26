package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSettingsTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.InkTextStyles

private val PreviewDestinations = listOf(
    R.drawable.ic_nav_home_filled,
    R.drawable.ic_nav_library_outline,
    R.drawable.ic_nav_stats_outline,
    R.drawable.ic_nav_apps_outline,
)

private val PreviewGrayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
internal fun DisplayModePreview(mode: DisplayMode, colors: ColorScheme, selected: Boolean) {
    val shape = RoundedCornerShape(InkRadius.medium)
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics {}.clip(shape).background(colors.background)
            .border(
                if (selected) InkSettingsTokens.previewSelectedStroke else InkBorder.default,
                if (selected) colors.primary else colors.outlineVariant,
                shape,
            )
            .padding(InkSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        Text(
            stringResource(R.string.brand_name),
            modifier = Modifier.padding(end = InkSettingsTokens.previewIconSize + InkSpacing.xs),
            style = InkSettingsTokens.previewBrandStyle,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.sm),
        ) {
            PreviewCover(mode)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
            ) {
                Text(
                    stringResource(R.string.display_preview_book_title),
                    style = InkTextStyles.labelMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.display_preview_read),
                    modifier = Modifier
                        .background(colors.primary, RoundedCornerShape(InkRadius.small))
                        .padding(horizontal = InkSpacing.sm, vertical = InkSpacing.xs),
                    style = InkTextStyles.labelSmall,
                    color = colors.onPrimary,
                    maxLines = 1,
                )
            }
        }
        PreviewNavigation(colors)
    }
}

@Composable
private fun PreviewCover(mode: DisplayMode) {
    Canvas(
        Modifier.size(InkSettingsTokens.previewCoverWidth, InkSettingsTokens.previewCoverHeight)
            .clip(RoundedCornerShape(InkRadius.small))
            .graphicsLayer {
                colorFilter = if (mode == DisplayMode.Grayscale) PreviewGrayscale else null
            },
    ) {
        drawRect(InkSettingsTokens.previewCoverSky)
        drawCircle(
            InkSettingsTokens.previewCoverSun,
            radius = size.width * .17f,
            center = Offset(size.width * .67f, size.height * .32f),
        )
        val hills = Path().apply {
            moveTo(0f, size.height * .64f)
            lineTo(size.width * .43f, size.height * .45f)
            lineTo(size.width, size.height * .75f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(hills, InkSettingsTokens.previewCoverLand)
    }
}

@Composable
private fun PreviewNavigation(colors: ColorScheme) {
    HorizontalDivider(thickness = InkBorder.thin, color = colors.outlineVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PreviewDestinations.forEachIndexed { index, drawable ->
            Icon(
                painterResource(drawable),
                contentDescription = null,
                modifier = Modifier.size(InkSettingsTokens.previewIconSize),
                tint = if (index == 0) colors.primary else colors.onSurface,
            )
        }
    }
}
