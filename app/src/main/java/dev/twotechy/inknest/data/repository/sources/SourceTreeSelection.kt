package dev.twotechy.inknest.data.repository.sources

data class SourceTreeSelection(
    val folder: SourceFolder,
    val uri: String,
    val flags: Int,
    val replacedTree: String? = null,
    val pairKoreader: Boolean = false,
)
