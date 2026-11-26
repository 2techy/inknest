package dev.twotechy.inknest.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object InkSettingsTokens {
    val rowMinHeight = 64.dp
    val choiceMinHeight = 56.dp
    val toggleMinHeight = 64.dp
    val rowPadding = InkSpacing.sm
    val sectionGap = InkSpacing.xl
    val headingGap = InkSpacing.sm
    val leadingSize = 32.dp
    val iconSize = 24.dp
    val chevronSize = 20.dp
    val selectionSize = 24.dp
    val selectionStroke = 1.5.dp
    val checkStroke = 1.8.dp
    val previewMinCardWidth = 104.dp
    val previewIconSize = 14.dp
    val previewBrandStyle = InkTextStyles.homeBrand.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val previewCoverWidth = 34.dp
    val previewCoverHeight = 50.dp
    val previewSelectedStroke = 2.dp
    val previewCoverSky = Color(0xFFDAE3E7)
    val previewCoverSun = Color(0xFFD79B53)
    val previewCoverLand = Color(0xFF4D6779)
}
