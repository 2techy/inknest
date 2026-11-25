package dev.twotechy.inknest.data.local.database.reading

import androidx.room3.DatabaseView

internal const val READING_BOOK_ROWS_SQL = """
    SELECT r.sourceId, r.snapshotKey, r.path, COALESCE(b.title, r.path) AS title,
        r.availability, b.author, r.progress, b.coverUri, r.status, r.lastReadAt, r.historyOrder,
        f.fingerprint, r.currentPage, r.totalPages, b.description
    FROM reading_items r JOIN book_files f ON f.id=r.fileId JOIN books b ON b.id=f.bookId
"""

@DatabaseView(value = READING_BOOK_ROWS_SQL, viewName = "reading_book_rows")
internal data class ReadingBookRow(
    val sourceId: String,
    val snapshotKey: String,
    val path: String,
    val title: String,
    val availability: String,
    val author: String? = null,
    val progress: Int? = null,
    val coverUri: String? = null,
    val status: String = "Unknown",
    val lastReadAt: Long? = null,
    val historyOrder: Int? = null,
    val fingerprint: String? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val description: String? = null,
)
