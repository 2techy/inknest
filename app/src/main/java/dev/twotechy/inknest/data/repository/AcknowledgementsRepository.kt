package dev.twotechy.inknest.data.repository

internal fun interface AcknowledgementsRepository {
    suspend fun read(): String
}
