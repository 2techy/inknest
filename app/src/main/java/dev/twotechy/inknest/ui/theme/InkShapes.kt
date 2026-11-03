package dev.twotechy.inknest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal object InkRadius {
    val small = 4.dp
    val medium = 8.dp
    val large = 12.dp
}

internal object InkBorder {
    val thin = 1.dp
    val default = 1.25.dp
}

internal val InkShapes = Shapes(
    extraSmall = RoundedCornerShape(InkRadius.small),
    small = RoundedCornerShape(InkRadius.medium),
    medium = RoundedCornerShape(InkRadius.medium),
    large = RoundedCornerShape(InkRadius.large),
    extraLarge = RoundedCornerShape(InkRadius.large),
)
