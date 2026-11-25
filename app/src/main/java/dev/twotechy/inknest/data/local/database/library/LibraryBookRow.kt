package dev.twotechy.inknest.data.local.database.library

import androidx.room3.DatabaseView

internal const val LIBRARY_BOOK_ROWS_SQL = """
    SELECT l.sourceId, l.folderKey, l.path, COALESCE(b.title, l.path) AS title,
        b.author, b.coverUri, f.fingerprint, l.progress, l.status, b.description, f.checksum
    FROM library_books l JOIN book_files f ON f.id=l.fileId JOIN books b ON b.id=f.bookId
"""

@DatabaseView(value = LIBRARY_BOOK_ROWS_SQL, viewName = "library_book_rows")
internal data class LibraryBookRow(
    val sourceId: String,
    val folderKey: String,
    val path: String,
    val title: String,
    val author: String? = null,
    val coverUri: String? = null,
    val fingerprint: String? = null,
    val progress: Int? = null,
    val status: String = "Unknown",
    val description: String? = null,
    val checksum: String? = null,
)
