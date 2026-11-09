package dev.twotechy.inknest.data.repository

import android.content.res.AssetManager
import dev.twotechy.inknest.model.ThirdPartyLicense
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AssetAcknowledgementsRepository(
    private val assets: AssetManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AcknowledgementsRepository {
    override suspend fun read(license: ThirdPartyLicense): String = withContext(ioDispatcher) {
        val paths = when (license) {
            ThirdPartyLicense.LuaJ -> listOf("licenses/luaj_notice.txt")

            ThirdPartyLicense.MaterialSymbols -> listOf(
                "licenses/material_symbols_notice.txt",
                "licenses/material_symbols_license.txt",
            )
        }
        paths.joinToString("\n\n") { path ->
            assets.open(path).bufferedReader().use { it.readText() }
        }
    }
}
