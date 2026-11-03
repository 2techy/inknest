package dev.twotechy.inknest.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.util.UUID
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class ReadingSourceConfigRepository(private val store: DataStore<Preferences>) {
    val config = store.data.map {
        ReadingSourceConfig(it[sourceId], it[koreader], it[books], it[sourcePackage])
    }.distinctUntilChanged()

    suspend fun setTree(folder: SourceFolder, uri: String) {
        store.edit {
            if (it[sourceId] == null) it[sourceId] = UUID.randomUUID().toString()
            it[if (folder == SourceFolder.KOReader) koreader else books] = uri
        }
    }

    private companion object {
        val sourceId = stringPreferencesKey("reading_source_id")
        val koreader = stringPreferencesKey("reading_koreader_tree")
        val books = stringPreferencesKey("reading_books_tree")
        val sourcePackage = stringPreferencesKey("reading_source_package")
    }
}
