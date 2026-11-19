package dev.twotechy.inknest.data.local.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.twotechy.inknest.data.local.database.library.LibraryBookEntity
import dev.twotechy.inknest.data.local.database.library.LibraryDao
import dev.twotechy.inknest.data.local.database.library.LibraryFolderEntity
import dev.twotechy.inknest.data.local.database.reading.ReadingBookEntity
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
        ReadingBookEntity::class, ReadingSourceEntity::class,
        LibraryBookEntity::class, LibraryFolderEntity::class,
        StatisticsEventEntity::class, StatisticsSourceEntity::class,
        StatisticsDayEntity::class, StatisticsImportEntity::class,
        StatisticsBookEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
internal abstract class ReadingDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
    abstract fun libraryDao(): LibraryDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        fun create(context: Context): ReadingDatabase = Room.databaseBuilder<ReadingDatabase>(
            context.applicationContext,
            "reading.db",
        ).fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
