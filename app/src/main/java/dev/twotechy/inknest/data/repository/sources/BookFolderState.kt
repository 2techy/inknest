package dev.twotechy.inknest.data.repository.sources

import dev.twotechy.inknest.model.SourceAccessState

data class BookFolderState(val uri: String, val access: SourceAccessState)
