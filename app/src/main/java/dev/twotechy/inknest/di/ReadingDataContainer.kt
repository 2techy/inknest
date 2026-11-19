package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.local.database.ReadingDatabase
import dev.twotechy.inknest.data.local.database.library.RoomLibraryInventoryStore
import dev.twotechy.inknest.data.local.database.reading.ReadingCache
import dev.twotechy.inknest.data.local.preferences.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.books.impl.RoomBookDetailsRepository
import dev.twotechy.inknest.data.repository.library.impl.SafLibraryRepository
import dev.twotechy.inknest.data.repository.reading.impl.RoomReadingRepository
import dev.twotechy.inknest.data.repository.reading.impl.SafReadingOpenRepository
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.sources.impl.SafSourceSettingsRepository
import dev.twotechy.inknest.data.repository.statistics.impl.KoreaderStatisticsRepository
import dev.twotechy.inknest.data.source.books.BookMetadata
import dev.twotechy.inknest.data.source.books.BookMetadataReader
import dev.twotechy.inknest.data.source.books.FileBookCoverStore
import dev.twotechy.inknest.data.source.books.impl.SafBookCatalogDataSource
import dev.twotechy.inknest.data.source.books.impl.SafBookDataSource
import dev.twotechy.inknest.data.source.koreader.impl.KoreaderCurrentReadingDataSource
import dev.twotechy.inknest.data.source.saf.SafSourceAccess
import dev.twotechy.inknest.data.source.saf.impl.SafTreeReader
import dev.twotechy.inknest.data.source.statistics.impl.KoreaderStatisticsDataSource
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.clock.SystemStatisticsClock
import dev.twotechy.inknest.platform.covers.AndroidBookCoverDecoder
import dev.twotechy.inknest.platform.covers.BookCoverLoader
import dev.twotechy.inknest.platform.reading.ReadingItemDispatcher
import dev.twotechy.inknest.platform.tracing.AppOperationTrace
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
    private val books = SafBookDataSource(files, appContext.cacheDir, ioDispatcher)
    private val database = ReadingDatabase.create(appContext)
    private val cache = ReadingCache(database.readingDao(), defaultDispatcher)
    private val coverDirectory = File(appContext.filesDir, "reading-covers")
    val coverLoader = BookCoverLoader(
        AndroidBookCoverDecoder(coverDirectory),
        ioDispatcher,
        maxBytes = (Runtime.getRuntime().maxMemory() / 8).coerceIn(
            1,
            Int.MAX_VALUE.toLong(),
        ).toInt(),
        parentScope = scope,
    )
    private val covers = FileBookCoverStore(coverDirectory, coverLoader::clear, ioDispatcher)
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
    val library = SafLibraryRepository(
        config,
        RoomLibraryInventoryStore(libraryDao),
        SafBookCatalogDataSource(appContext.contentResolver, ioDispatcher),
        scope,
        reading,
        tracer = AppOperationTrace.tracer,
        now = now,
        readMetadata = libraryMetadata::readMetadata,
    )
    private val statisticsSnapshot =
        KoreaderStatisticsDataSource(
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
    val bookDetails = RoomBookDetailsRepository(
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
