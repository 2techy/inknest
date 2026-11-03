package dev.twotechy.inknest.data.repository

import android.content.res.AssetManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AssetAcknowledgementsRepository(
    private val assets: AssetManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AcknowledgementsRepository {
    override suspend fun read(): String = withContext(ioDispatcher) {
        listOf("licenses/material_symbols_notice.txt", "licenses/material_symbols_license.txt")
            .joinToString("\n\n") { path ->
                assets.open(path).bufferedReader().use { it.readText() }
            }
    }
}
