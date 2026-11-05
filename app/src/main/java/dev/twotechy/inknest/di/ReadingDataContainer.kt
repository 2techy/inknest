package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.CachedReadingRepository
import dev.twotechy.inknest.data.repository.FolderLibraryRepository
import dev.twotechy.inknest.data.repository.KoreaderStatisticsRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SafSourceSettingsRepository
import dev.twotechy.inknest.data.source.BookCoverCache
import dev.twotechy.inknest.data.source.BookMetadata
import dev.twotechy.inknest.data.source.BookMetadataReader
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.KoreaderCurrentSource
import dev.twotechy.inknest.data.source.SafBookCatalogSource
import dev.twotechy.inknest.data.source.SafSourceAccess
import dev.twotechy.inknest.data.source.SafTreeReader
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotProbe
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.ReadingItemOpener
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

// One application-scoped graph for reading data; shares the existing DataStore and Room instance.
internal class ReadingDataContainer(context: Context, scope: CoroutineScope) {
    private val appContext = context.applicationContext
    val config = ReadingSourceConfigRepository(appContext.userPreferencesDataStore)
    val access = SafSourceAccess(appContext.contentResolver)
    private val files = SafTreeReader(appContext.contentResolver)
    private val books = BookProbeDataSource(files, appContext.cacheDir)
    private val database = ReadingDatabase.create(appContext)
    private val cache = ReadingCache(database.readingDao())
    private val covers = BookCoverCache(File(appContext.filesDir, "reading-covers"))
    private val metadata = BookMetadataReader(books, covers, cache::metadata)
    private val transition = Mutex()
    val reading = CachedReadingRepository(
        config,
        cache,
        KoreaderCurrentSource(files, books, access = access, metadataReader = metadata),
        scope,
        covers,
        transition,
    )
    private val libraryDao = database.libraryDao()
    private val libraryMetadata = BookMetadataReader(books, covers, ::cachedMetadata)
    val library = FolderLibraryRepository(
        config,
        libraryDao,
        SafBookCatalogSource(appContext.contentResolver),
        scope,
        reading,
        libraryMetadata::readBook,
    )
    private val statisticsSnapshot = StatisticsSnapshotProbe(files, appContext.cacheDir)
    val statistics = KoreaderStatisticsRepository(
        config,
        database.statisticsDao(),
        statisticsSnapshot::events,
        scope,
        transition,
    )
    val settings = SafSourceSettingsRepository(
        config,
        access,
        reading,
        appContext.getDatabasePath("reading.db"),
        files,
        scope,
        library,
        covers,
        statistics,
    )
    val opener = ReadingItemOpener(appContext, config, books)

    private suspend fun cachedMetadata(config: ReadingSourceConfig, path: String): BookMetadata? {
        val source = config.sourceId ?: return null
        val tree = config.booksTreeUri ?: return cache.metadata(config, path)
        val saved = libraryDao.book(source, tree, path)?.takeIf { it.fingerprint != null }
        return saved?.let { BookMetadata(it.fingerprint, it.title, it.author, it.coverUri) }
            ?: cache.metadata(config, path)
    }
}
