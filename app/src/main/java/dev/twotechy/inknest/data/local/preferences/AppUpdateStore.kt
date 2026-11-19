package dev.twotechy.inknest.data.local.preferences

import dev.twotechy.inknest.model.UpdateSnapshot
import kotlinx.coroutines.flow.Flow

internal interface AppUpdateStore {
    val snapshots: Flow<UpdateSnapshot>
    suspend fun update(transform: (UpdateSnapshot) -> UpdateSnapshot)
}
