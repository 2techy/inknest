package dev.twotechy.inknest.ui.settings

import androidx.compose.ui.text.AnnotatedString

internal data class ReleaseNoteBlock(
    val text: AnnotatedString,
    val style: ReleaseNoteStyle = ReleaseNoteStyle.Body,
    val depth: Int = 0,
    val marker: String? = null,
)
