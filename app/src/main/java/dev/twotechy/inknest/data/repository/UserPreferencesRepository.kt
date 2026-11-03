package dev.twotechy.inknest.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface UserPreferencesRepository {
    val pinnedComponentIds: Flow<List<String>>
    val readError: Flow<String?> get() = flowOf(null)
    fun retryRead() = Unit

    suspend fun pin(componentId: String)
    suspend fun unpin(componentId: String)
    suspend fun movePin(componentId: String, offset: Int)
    suspend fun pruneStalePins(validComponentIds: Set<String>)
}
