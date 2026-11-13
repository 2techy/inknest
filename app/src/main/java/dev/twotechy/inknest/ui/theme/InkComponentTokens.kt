package dev.twotechy.inknest.ui.theme

import androidx.compose.ui.unit.dp

// Component dimensions are not spacing increments. Optical strokes and the 600dp breakpoint
// are intentional exceptions to the spacing scale.
internal object InkHeaderTokens {
    val minHeight = 48.dp
    val iconSize = 24.dp
    val touchTarget = 48.dp

    // ic_back's leftmost path point is x=8, with a 1.5-wide round stroke.
    val backIconStartInset = 7.25.dp
}
internal object InkLayoutTokens {
    val narrowBreakpoint = 360.dp
    val railBreakpoint = 600.dp
    val maxContentWidth = 800.dp
    val narrowGutter = InkSpacing.md
    val compactGutter = InkSpacing.lg
    val expandedGutter = InkSpacing.xxxl
    val topPadding = InkSpacing.xs
    val bottomPadding = InkSpacing.lg
    val headerContentGap = InkSpacing.sm
    val sectionGap = InkSpacing.xxl
    val sectionContentGap = InkSpacing.md
    val cardPadding = InkSpacing.lg
    val compactCardPadding = InkSpacing.md
}
internal object InkAppGridTokens {
    val minCellWidth = 84.dp
    val horizontalGap = InkSpacing.md
    val verticalGap = InkSpacing.lg
    val cellPadding = InkSpacing.xs
    val iconFrame = 48.dp
    val iconSize = 36.dp
    val listIconFrame = 40.dp
    val listIconSize = 28.dp
    val iconLabelGap = InkSpacing.sm
}
internal object InkSettingsTokens {
    val rowMinHeight = 64.dp
    val choiceMinHeight = 56.dp
    val toggleMinHeight = 64.dp
    val rowPadding = InkSpacing.md
    val leadingSize = 32.dp
    val iconSize = 24.dp
    val chevronSize = 20.dp
    val selectionSize = 24.dp
    val selectionStroke = 1.5.dp
    val checkStroke = 1.8.dp
}
internal object InkButtonTokens {
    val minHeight = 48.dp
    val minWidth = 112.dp
    val horizontalPadding = InkSpacing.lg
    val verticalPadding = InkSpacing.md
}
internal object InkUpdateTokens {
    val metadataMinColumnWidth = 160.dp
}
internal object InkNavigationTokens {
    val iconSize = 24.dp
    val minHeight = 64.dp
    val compactMinHeight = 48.dp
    val iconLabelGap = InkSpacing.xs
    val markerWidth = 32.dp
    val markerHeight = 3.dp
    val railWidth = 80.dp
    val railMinWidth = 104.dp
    val railMaxWidth = 160.dp
}
internal object InkReadingTokens {
    val coverMinWidth = 64.dp
    val coverMaxWidth = 112.dp
    val minDetailsWidth = 150.dp
    val miniStatMinWidth = 100.dp
    val progressHeight = 4.dp
    val artworkStroke = 1.dp
}

internal object InkMenuTokens {
    val minWidth = 160.dp
    val maxWidth = 280.dp
}

internal object InkHomeTokens {
    val noticeMinHeight = 64.dp
}

internal object InkLibraryTokens {
    val minCellWidth = 112.dp
    val maxCoverWidth = 144.dp
    val listCoverWidth = 48.dp
}

internal object InkStatsTokens {
    val chartHeight = 112.dp
    val chartAxisWidth = 48.dp
    val maxBarWidth = 24.dp
    val heatmapLabelWidth = 48.dp
    val heatmapHorizontalGap = 3.dp
    val heatmapVerticalGap = 2.dp
    val heatmapCornerRadius = 2.dp
    val heatmapEmptyStroke = 0.75.dp
    val heatmapCellSize = 10.dp
    val rangeMarker = 2.dp
}

internal object InkChoiceTokens {
    val minHeight = 32.dp
    val horizontalPadding = InkSpacing.md
    val verticalPadding = InkSpacing.sm
}

internal object InkOnboardingTokens {
    val progressHeight = 3.dp
}
