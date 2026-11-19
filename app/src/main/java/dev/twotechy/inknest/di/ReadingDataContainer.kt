package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.reading.RoomLibraryInventoryStore
import dev.twotechy.inknest.data.local.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.CachedBookDetailsRepository
import dev.twotechy.inknest.data.repository.FolderLibraryRepository
import dev.twotechy.inknest.data.repository.KoreaderStatisticsRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.RoomReadingRepository
import dev.twotechy.inknest.data.repository.SafReadingOpenRepository
import dev.twotechy.inknest.data.repository.SafSourceSettingsRepository
import dev.twotechy.inknest.data.source.books.BookCoverCache
import dev.twotechy.inknest.data.source.books.BookMetadata
import dev.twotechy.inknest.data.source.books.BookMetadataReader
import dev.twotechy.inknest.data.source.books.impl.BookProbeDataSource
import dev.twotechy.inknest.data.source.books.impl.SafBookCatalogSource
import dev.twotechy.inknest.data.source.koreader.impl.KoreaderCurrentReadingDataSource
import dev.twotechy.inknest.data.source.saf.SafSourceAccess
import dev.twotechy.inknest.data.source.saf.impl.SafTreeReader
import dev.twotechy.inknest.data.source.stats.impl.StatisticsSnapshotProbe
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.AndroidBookCoverSource
import dev.twotechy.inknest.platform.AppOperationTrace
import dev.twotechy.inknest.platform.BookCoverLoader
import dev.twotechy.inknest.platform.ReadingItemDispatcher
import dev.twotechy.inknest.platform.clock.SystemStatisticsClock
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex

// One application-scoped graph for reading data; shares the existing DataStore and Room instance.
internal class ReadingDataContainer(
    context: Context,
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    now: () -> Long = System::currentTimeMillis,
) {
    val statisticsClock = SystemStatisticsClock(scope, defaultDispatcher)
    private val appContext = context.applicationContext
    val config = ReadingSourceConfigRepository(appContext.userPreferencesDataStore)
    val access = SafSourceAccess(appContext.contentResolver, ioDispatcher)
    private val files = SafTreeReader(appContext.contentResolver, ioDispatcher)
    private val books = BookProbeDataSource(files, appContext.cacheDir, ioDispatcher)
    private val database = ReadingDatabase.create(appContext)
    private val cache = ReadingCache(database.readingDao(), defaultDispatcher)
    private val coverDirectory = File(appContext.filesDir, "reading-covers")
    val coverLoader = BookCoverLoader(AndroidBookCoverSource(coverDirectory))
    private val covers = BookCoverCache(coverDirectory, coverLoader::clear, ioDispatcher)
    private val metadata = BookMetadataReader(books, covers, cache::metadata)
    private val transition = Mutex()
    val reading = RoomReadingRepository(
        config,
        cache,
        KoreaderCurrentReadingDataSource(
            files,
            books,
            access = access,
            metadataReader = metadata,
            ioDispatcher = ioDispatcher,
            now = now,
        ),
        scope,
        covers,
        transition,
        now = now,
        permissionSnapshot = files::permissionSnapshot,
        tracer = AppOperationTrace.tracer,
    )
    private val libraryDao = database.libraryDao()
    private val libraryMetadata = BookMetadataReader(books, covers, ::cachedMetadata)
    val library = FolderLibraryRepository(
        config,
        RoomLibraryInventoryStore(libraryDao),
        SafBookCatalogSource(appContext.contentResolver, ioDispatcher),
        scope,
        reading,
        tracer = AppOperationTrace.tracer,
        now = now,
        enrich = libraryMetadata::readBook,
    )
    private val statisticsSnapshot =
        StatisticsSnapshotProbe(
            files,
            appContext.cacheDir,
            ioDispatcher,
            tracer = AppOperationTrace.tracer,
        )
    val statistics = KoreaderStatisticsRepository(
        config,
        database.statisticsDao(),
        statisticsSnapshot,
        scope,
        transition,
        statisticsClock,
        defaultDispatcher,
        permissionSnapshot = files::permissionSnapshot,
        tracer = AppOperationTrace.tracer,
    )
    val bookDetails = CachedBookDetailsRepository(
        library,
        config,
        libraryDao,
        database.statisticsDao(),
        statistics,
        books,
        statisticsClock,
        ioDispatcher,
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
    val opening = SafReadingOpenRepository(config, books, ioDispatcher)
    val dispatcher =
        ReadingItemDispatcher(
            appContext,
            opening,
            ioDispatcher = ioDispatcher,
            mainDispatcher = mainDispatcher,
        )

    private suspend fun cachedMetadata(config: ReadingSourceConfig, path: String): BookMetadata? {
        val source = config.sourceId ?: return null
        val saved = config.booksTreeUris.sorted().firstNotNullOfOrNull { tree ->
            libraryDao.book(source, tree, path)?.takeIf { it.fingerprint != null }
        }
        return saved?.let {
            BookMetadata(it.fingerprint, it.title, it.author, it.coverUri, it.description)
        }
            ?: cache.metadata(config, path)
    }
}
