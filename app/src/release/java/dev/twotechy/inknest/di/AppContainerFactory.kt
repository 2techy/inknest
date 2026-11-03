package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.repository.UnconnectedReadingRepository
import dev.twotechy.inknest.data.repository.UnconnectedReadingStatsRepository

internal fun createAppContainer(context: Context) = createRuntimeContainer(
    context,
    UnconnectedReadingRepository(),
    UnconnectedReadingStatsRepository(),
)
