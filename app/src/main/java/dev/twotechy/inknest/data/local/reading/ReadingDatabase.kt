package dev.twotechy.inknest.data.local.reading

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        ReadingBookEntity::class, ReadingSourceEntity::class,
        LibraryBookEntity::class, LibraryFolderEntity::class,
        StatisticsEventEntity::class, StatisticsSourceEntity::class,
        StatisticsDayEntity::class, StatisticsImportEntity::class,
    ],
    version = 6,
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
