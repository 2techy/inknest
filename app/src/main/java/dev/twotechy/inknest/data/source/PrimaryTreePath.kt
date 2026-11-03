package dev.twotechy.inknest.data.source

// Document IDs belong to ExternalStorageProvider, not arbitrary content providers.
internal data class PrimaryTreePath(val segments: List<String>) {
    fun relativeTo(root: PrimaryTreePath): List<String>? = if (segments.take(root.segments.size) ==
        root.segments
    ) {
        segments.drop(root.segments.size)
    } else {
        null
    }

    companion object {
        fun parse(documentId: String): PrimaryTreePath? {
            if (!documentId.startsWith("primary:")) return null
            val path = documentId.removePrefix("primary:")
            val segments = if (path.isEmpty()) emptyList() else path.split('/')
            if (segments.any {
                    it.isEmpty() || it == "." || it == ".." || '\u0000' in it
                }
            ) {
                return null
            }
            return PrimaryTreePath(segments)
        }
    }
}
