package dev.twotechy.inknest.data.repository.sources.impl

import dev.twotechy.inknest.data.repository.sources.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.sources.SourceSettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class UnconnectedSourceSettingsRepository : SourceSettingsRepository {
    override val state = MutableStateFlow(SourceSettingsState()).asStateFlow()
    override suspend fun disconnect(): Unit = error("Reading integration unavailable")
    override suspend fun refresh(): Unit = error("Reading integration unavailable")
    override suspend fun clearCache(): Unit = error("Reading integration unavailable")
}
