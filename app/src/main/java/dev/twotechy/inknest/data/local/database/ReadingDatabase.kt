package dev.twotechy.inknest.data.local.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.twotechy.inknest.data.local.database.books.BookEntity
import dev.twotechy.inknest.data.local.database.books.BookFileEntity
import dev.twotechy.inknest.data.local.database.library.LibraryBookEntity
import dev.twotechy.inknest.data.local.database.library.LibraryBookRow
import dev.twotechy.inknest.data.local.database.library.LibraryDao
import dev.twotechy.inknest.data.local.database.library.LibraryFolderEntity
import dev.twotechy.inknest.data.local.database.reading.ReadingBookEntity
import dev.twotechy.inknest.data.local.database.reading.ReadingBookRow
import dev.twotechy.inknest.data.local.database.reading.ReadingDao
import dev.twotechy.inknest.data.local.database.reading.ReadingSourceEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsBookEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsDao
import dev.twotechy.inknest.data.local.database.statistics.StatisticsDayEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsEventEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsImportEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsSourceEntity
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        BookEntity::class, BookFileEntity::class,
        ReadingBookEntity::class, ReadingSourceEntity::class,
        LibraryBookEntity::class, LibraryFolderEntity::class,
        StatisticsEventEntity::class, StatisticsSourceEntity::class,
        StatisticsDayEntity::class, StatisticsImportEntity::class,
        StatisticsBookEntity::class,
    ],
    views = [ReadingBookRow::class, LibraryBookRow::class],
    version = 9,
    exportSchema = true,
)
internal abstract class ReadingDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
    abstract fun libraryDao(): LibraryDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        fun create(context: Context, name: String = "reading.db"): ReadingDatabase =
            Room.databaseBuilder<ReadingDatabase>(
                context.applicationContext,
                name,
            ).fallbackToDestructiveMigration(dropAllTables = true)
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
