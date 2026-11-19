package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.data.repository.sources.SourceFolder

internal data class SourceConnectionActions(
    val selectApp: (String) -> Unit,
    val selectFolder: (SourceFolder) -> Unit,
    val replaceBookFolder: (String) -> Unit,
    val removeBookFolder: (String) -> Unit,
)
