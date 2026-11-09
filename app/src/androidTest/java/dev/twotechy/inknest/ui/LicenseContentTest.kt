package dev.twotechy.inknest.ui

import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.repository.AssetAcknowledgementsRepository
import dev.twotechy.inknest.model.ThirdPartyLicense
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LicenseContentTest {
    @Test fun eachComponentRetainsItsCompleteBundledLicense() = runBlocking {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val repository = AssetAcknowledgementsRepository(assets)
        val luaj = assets.open("licenses/luaj_notice.txt").bufferedReader().use { it.readText() }
        val symbols = assets.open("licenses/material_symbols_notice.txt").bufferedReader().use {
            it.readText()
        }
        val apache = assets.open("licenses/material_symbols_license.txt").bufferedReader().use {
            it.readText()
        }
        assertEquals(luaj, repository.read(ThirdPartyLicense.LuaJ))
        assertEquals("$symbols\n\n$apache", repository.read(ThirdPartyLicense.MaterialSymbols))
    }
}
