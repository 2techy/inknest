package dev.twotechy.inknest.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.twotechy.inknest.model.ReadingOpenMode
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

internal class ReadingSourceConfigRepository(private val store: DataStore<Preferences>) {
    val config = store.data.retryWhen { cause, attempt ->
        if (cause !is IOException) return@retryWhen false
        delay(500L * (attempt + 1).coerceAtMost(20))
        true
    }.map {
        ReadingSourceConfig(
            it[sourceId],
            it[koreader],
            it[books],
            it[sourcePackage],
            ReadingOpenMode.entries.find { mode -> mode.name == it[openMode] }
                ?: ReadingOpenMode.Unverified,
            it[appVersion],
        )
    }.distinctUntilChanged()

    suspend fun setTree(folder: SourceFolder, uri: String) {
        store.edit {
            if (it[sourceId] == null || (folder == SourceFolder.KOReader && it[koreader] != uri)) {
                it[sourceId] = UUID.randomUUID().toString()
            }
            it[if (folder == SourceFolder.KOReader) koreader else books] = uri
        }
    }

    fun treeCandidate(
        before: ReadingSourceConfig,
        folder: SourceFolder,
        uri: String,
    ): ReadingSourceConfig = before.copy(
        sourceId = if (before.sourceId == null ||
            (folder == SourceFolder.KOReader && before.koreaderTreeUri != uri)
        ) {
            UUID.randomUUID().toString()
        } else {
            before.sourceId
        },
        koreaderTreeUri = if (folder == SourceFolder.KOReader) uri else before.koreaderTreeUri,
        booksTreeUri = if (folder == SourceFolder.Books) uri else before.booksTreeUri,
    )

    suspend fun activateTree(before: ReadingSourceConfig, candidate: ReadingSourceConfig) {
        store.edit { values ->
            check(
                values[sourceId] == before.sourceId && values[koreader] == before.koreaderTreeUri &&
                    values[books] == before.booksTreeUri,
            )
            values[sourceId] = checkNotNull(candidate.sourceId)
            candidate.koreaderTreeUri?.let { values[koreader] = it }
            candidate.booksTreeUri?.let { values[books] = it }
        }
    }

    suspend fun setSourcePackage(packageName: String) {
        require(packageName.isNotBlank() && '/' !in packageName)
        store.edit {
            if (it[sourcePackage] != packageName) {
                it.remove(openMode)
                it.remove(appVersion)
            }
            it[sourcePackage] = packageName
        }
    }

    suspend fun disconnect() {
        store.edit { values ->
            listOf(sourceId, koreader, books, sourcePackage, openMode, appVersion).forEach {
                values.remove(it)
            }
        }
    }

    suspend fun recordOpenMode(mode: ReadingOpenMode, version: String) {
        require(mode != ReadingOpenMode.Unverified)
        store.edit {
            it[openMode] = mode.name
            it[appVersion] = version
        }
    }

    private companion object {
        val sourceId = stringPreferencesKey("reading_source_id")
        val koreader = stringPreferencesKey("reading_koreader_tree")
        val books = stringPreferencesKey("reading_books_tree")
        val openMode = stringPreferencesKey("reading_open_mode")
        val appVersion = stringPreferencesKey("reading_open_version")
        val sourcePackage = stringPreferencesKey("reading_source_package")
    }
}
