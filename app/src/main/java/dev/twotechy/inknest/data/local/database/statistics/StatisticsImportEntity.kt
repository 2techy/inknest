package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Entity

@Entity(tableName = "statistics_imports", primaryKeys = ["id"])
internal data class StatisticsImportEntity(val id: String, val source: String)
