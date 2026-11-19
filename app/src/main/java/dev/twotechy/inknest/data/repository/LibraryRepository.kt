package dev.twotechy.inknest.data.repository

import kotlinx.coroutines.flow.Flow

internal interface LibraryRepository {
    val catalog: Flow<LibraryCatalog>
    suspend fun refresh()
}
