package dev.twotechy.inknest.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.twotechy.inknest.data.source.AppReleaseJson
import dev.twotechy.inknest.model.UpdateSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal interface AppUpdateStore {
    val snapshots: Flow<UpdateSnapshot>
    suspend fun update(transform: (UpdateSnapshot) -> UpdateSnapshot)
}

internal class DataStoreAppUpdateStore(private val store: DataStore<Preferences>) : AppUpdateStore {
    override val snapshots = store.data.map(::decode).distinctUntilChanged()

    override suspend fun update(transform: (UpdateSnapshot) -> UpdateSnapshot) {
        store.edit { preferences ->
            val next = transform(decode(preferences))
            preferences[AUTOMATIC] = next.automatic
            preferences.setOrRemove(ATTEMPTED, next.attemptedAt)
            preferences.setOrRemove(CHECKED, next.checkedAt)
            preferences.setOrRemove(RETRY_AT, next.retryAt)
            preferences.setOrRemove(RELEASE, next.release?.let(AppReleaseJson::encode))
            preferences.setOrRemove(SHA256, next.downloadedSha256)
        }
    }

    private fun decode(preferences: Preferences): UpdateSnapshot = UpdateSnapshot(
        automatic = preferences[AUTOMATIC] ?: true,
        attemptedAt = preferences[ATTEMPTED],
        checkedAt = preferences[CHECKED],
        retryAt = preferences[RETRY_AT],
        release = preferences[RELEASE]?.let(AppReleaseJson::decode),
        downloadedSha256 = preferences[SHA256],
    )

    private fun <T> androidx.datastore.preferences.core.MutablePreferences.setOrRemove(
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val AUTOMATIC = booleanPreferencesKey("update_automatic")
        val ATTEMPTED = longPreferencesKey("update_attempted_at")
        val CHECKED = longPreferencesKey("update_checked_at")
        val RETRY_AT = longPreferencesKey("update_retry_at")
        val RELEASE = stringPreferencesKey("update_release")
        val SHA256 = stringPreferencesKey("update_sha256")
    }
}
