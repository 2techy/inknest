package dev.twotechy.inknest.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
            it[books].orEmpty(),
            it[sourcePackage],
        )
    }.distinctUntilChanged()

    fun treeCandidate(
        before: ReadingSourceConfig,
        folder: SourceFolder,
        uri: String,
        replacedTree: String? = null,
    ): ReadingSourceConfig {
        val updated = before.copy(
            koreaderTreeUri = if (folder == SourceFolder.KOReader) uri else before.koreaderTreeUri,
            booksTreeUris = if (folder == SourceFolder.Books) {
                before.booksTreeUris - listOfNotNull(replacedTree).toSet() + uri
            } else {
                before.booksTreeUris
            },
        )
        val identity = before.sourceId?.takeIf { before.koreaderTreeUri == updated.koreaderTreeUri }
        return updated.copy(sourceId = identity ?: UUID.randomUUID().toString())
    }

    suspend fun activateTree(before: ReadingSourceConfig, candidate: ReadingSourceConfig) {
        store.edit { values ->
            check(
                values[sourceId] == before.sourceId && values[koreader] == before.koreaderTreeUri &&
                    values[books].orEmpty() == before.booksTreeUris,
            )
            values[sourceId] = checkNotNull(candidate.sourceId)
            candidate.koreaderTreeUri?.let { values[koreader] = it }
            values[books] = candidate.booksTreeUris
        }
    }

    suspend fun setSourcePackage(packageName: String) {
        require(packageName.isNotBlank() && '/' !in packageName)
        store.edit {
            it[sourcePackage] = packageName
        }
    }

    suspend fun disconnect() {
        store.edit { values ->
            listOf(sourceId, koreader, books, sourcePackage).forEach {
                values.remove(it)
            }
        }
    }

    private companion object {
        val sourceId = stringPreferencesKey("reading_source_id")
        val koreader = stringPreferencesKey("reading_koreader_tree")
        val books = stringSetPreferencesKey("reading_books_trees")
        val sourcePackage = stringPreferencesKey("reading_source_package")
    }
}
