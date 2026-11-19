package dev.twotechy.inknest.data.repository.library

import kotlinx.coroutines.flow.Flow

internal interface LibraryRepository {
    val catalog: Flow<LibraryCatalog>
    suspend fun refresh()
}
