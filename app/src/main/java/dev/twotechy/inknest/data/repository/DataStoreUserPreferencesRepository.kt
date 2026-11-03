package dev.twotechy.inknest.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

internal class DataStoreUserPreferencesRepository(private val dataStore: DataStore<Preferences>) :
    UserPreferencesRepository {

    private val preferencesError = MutableStateFlow<String?>(null)
    private val readRetry = MutableStateFlow(0L)
    override val readError = preferencesError.asStateFlow()

    private val preferences = dataStore.data
        .map { it.also { preferencesError.value = null } }
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            preferencesError.value = "Unable to read preferences. Retrying…"
            val generation = readRetry.value
            withTimeoutOrNull(1_000L * (attempt + 1).coerceAtMost(30)) {
                readRetry.first { it != generation }
            }
            true
        }

    override val pinnedComponentIds = preferences.map {
        decodeList(it[KEY_PINNED_COMPONENT_IDS])
    }.distinctUntilChanged()

    override val appPreferences = preferences.map {
        AppPreferences(
            displayMode = DisplayMode.entries.find { mode -> mode.name == it[KEY_DISPLAY_MODE] }
                ?: DisplayMode.EInk,
            showHomeStats = it[KEY_SHOW_STATS] ?: true,
            showHomePinnedApps = it[KEY_SHOW_PINS] ?: true,
            onboardingCompleted = it[KEY_ONBOARDING_COMPLETED] ?: false,
        )
    }.distinctUntilChanged()

    override suspend fun setDisplayMode(mode: DisplayMode) {
        dataStore.edit { it[KEY_DISPLAY_MODE] = mode.name }
    }
    override suspend fun setShowHomeStats(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_STATS] = show }
    }
    override suspend fun setShowHomePinnedApps(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_PINS] = show }
    }
    override suspend fun completeOnboarding() {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = true }
    }

    override fun retryRead() {
        readRetry.update { it + 1 }
    }

    override suspend fun pin(componentId: String) {
        dataStore.edit { preferences ->
            val current = decodeList(preferences[KEY_PINNED_COMPONENT_IDS]).toMutableList()
            if (!current.contains(componentId)) {
                current.add(componentId)
                preferences[KEY_PINNED_COMPONENT_IDS] = encodeList(current)
            }
        }
    }

    override suspend fun unpin(componentId: String) {
        dataStore.edit { preferences ->
            val current = decodeList(preferences[KEY_PINNED_COMPONENT_IDS]).toMutableList()
            if (current.remove(componentId)) {
                preferences[KEY_PINNED_COMPONENT_IDS] = encodeList(current)
            }
        }
    }

    override suspend fun movePin(componentId: String, offset: Int) {
        dataStore.edit { preferences ->
            val current = decodeList(preferences[KEY_PINNED_COMPONENT_IDS]).toMutableList()
            val fromIndex = current.indexOf(componentId)
            val toIndex = fromIndex + offset
            if (fromIndex in current.indices && toIndex in current.indices &&
                fromIndex != toIndex
            ) {
                val item = current.removeAt(fromIndex)
                current.add(toIndex, item)
                preferences[KEY_PINNED_COMPONENT_IDS] = encodeList(current)
            }
        }
    }

    override suspend fun pruneStalePins(validComponentIds: Set<String>) {
        dataStore.edit { preferences ->
            val current = decodeList(preferences[KEY_PINNED_COMPONENT_IDS])
            val filtered = current.filter { validComponentIds.contains(it) }
            if (filtered.size != current.size) {
                preferences[KEY_PINNED_COMPONENT_IDS] = encodeList(filtered)
            }
        }
    }

    internal companion object {
        val KEY_PINNED_COMPONENT_IDS = stringPreferencesKey("pinned_component_ids")
        private val KEY_DISPLAY_MODE = stringPreferencesKey("display_mode")
        private val KEY_SHOW_STATS = booleanPreferencesKey("show_home_stats")
        private val KEY_SHOW_PINS = booleanPreferencesKey("show_home_pinned_apps")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private const val DELIMITER = "\n"

        fun encodeList(items: List<String>): String = items.joinToString(DELIMITER)

        fun decodeList(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(DELIMITER).filter { it.isNotBlank() }.distinct()
        }
    }
}
