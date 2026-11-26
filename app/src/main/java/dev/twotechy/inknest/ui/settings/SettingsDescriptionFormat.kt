package dev.twotechy.inknest.ui.settings

import androidx.compose.ui.text.TextStyle
import dev.twotechy.inknest.ui.theme.InkTextStyles

internal enum class SettingsDescriptionFormat(val style: TextStyle, val maxLines: Int) {
    Summary(InkTextStyles.bodySmall, 2),
    FullSummary(InkTextStyles.bodySmall, Int.MAX_VALUE),
    FolderDetails(InkTextStyles.bodyMedium, Int.MAX_VALUE),
}
