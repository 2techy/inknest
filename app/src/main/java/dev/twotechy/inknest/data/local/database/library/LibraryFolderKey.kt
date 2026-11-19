package dev.twotechy.inknest.data.local.database.library

internal data class LibraryFolderKey(val source: String, val tree: String)

// Inventory operations are scoped to one configured root; Room owns SQL and transactions.
