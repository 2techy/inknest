package dev.twotechy.inknest.ui.settings

internal data class ReleaseNotesDocument(
    val source: String = "",
    val blocks: List<ReleaseNoteBlock> = emptyList(),
)
