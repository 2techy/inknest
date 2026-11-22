package dev.twotechy.inknest.ui.shared

import dev.twotechy.inknest.data.repository.sources.SourceFolder

internal data class SourceFolderActions(
    val selectFolder: (SourceFolder) -> Unit,
    val replaceBookFolder: (String) -> Unit,
    val removeBookFolder: (String) -> Unit,
)
