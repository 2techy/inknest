package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.PinnedApp
import kotlinx.coroutines.flow.Flow

// Listing, search and launching are added in M8; Home only needs pins today.
interface InstalledAppsRepository {
    val pinnedApps: Flow<List<PinnedApp>>
}
