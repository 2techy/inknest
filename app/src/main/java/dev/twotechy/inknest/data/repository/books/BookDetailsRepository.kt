package dev.twotechy.inknest.data.repository.books

import dev.twotechy.inknest.model.BookDetails
import dev.twotechy.inknest.model.ReadingItemId
import kotlinx.coroutines.flow.Flow

interface BookDetailsRepository {
    fun observe(id: ReadingItemId, historyPage: Int): Flow<BookDetails>
    fun retry()
}
