package dev.twotechy.inknest.data.source.lua

import dev.twotechy.inknest.data.source.koreader.lua.BoundedLuaLoader
import dev.twotechy.inknest.data.source.koreader.lua.LuaFailure
import dev.twotechy.inknest.data.source.koreader.lua.LuaSourceException
import dev.twotechy.inknest.data.source.koreader.lua.ReaderLuaDataSource
import dev.twotechy.inknest.data.source.koreader.lua.SidecarDto
import java.io.ByteArrayInputStream
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReaderLuaDataSourceTest {
    private fun source(text: String) = ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))

    @Test fun `settings distinguish absent current and invalid type`() = runTest {
        val reader = ReaderLuaDataSource()
        assertNull(reader.lastFile(source("return {}")))
        assertEquals(
            "/webdav/Tiếng Việt.epub",
            reader.lastFile(source("return {lastfile='/webdav/Tiếng Việt.epub'}")),
        )
        for (script in listOf(
            "return {lastfile=42}",
            "return {lastfile=''}",
            "return false",
            "broken lua",
        )) {
            try {
                reader.lastFile(source(script))
                fail("Expected invalid source")
            } catch (
                _: LuaSourceException,
            ) { }
        }
    }

    @Test fun `history preserves array order and rejects corrupt entries`() = runTest {
        val reader = ReaderLuaDataSource()
        val entries = reader.history(
            source("return {{file='b.epub',time=1},{file='a.epub',time=9}}"),
        )
        assertEquals(listOf("b.epub", "a.epub"), entries.map { it.file })
        val partial = reader.trackedHistory(
            source(
                "return {{file='/a',time=2},false,{file='/b',time=1e30},{file='/c',time=3}}",
            ),
        )
        assertEquals(listOf("/a", "/c"), partial.entries.map { it.file })
        assertEquals(2, partial.skipped)
        assertTrue(reader.history(source("return {}")).isEmpty())
        for (script in listOf(
            "return {{file='a',time='9'}}",
            "return {[2]={file='a',time=1}}",
            "return {false}",
        )) {
            try {
                reader.history(source(script))
                fail("Expected corrupt history")
            } catch (
                _: LuaSourceException,
            ) { }
        }
    }

    @Test fun `sandbox has no host libraries and runaway cannot poison later parses`() = runTest {
        val loader = BoundedLuaLoader(10_000)
        val script = "return {blocked = io==nil and os==nil and package==nil and " +
            "luajava==nil and load==nil and debug==nil and require==nil}"
        assertTrue(loader.read(source(script)) { it.rawget("blocked").toboolean() })
        try {
            loader.read(source("while true do end")) { }
            fail("Expected instruction limit")
        } catch (
            error: LuaSourceException,
        ) {
            assertEquals(LuaFailure.InstructionLimit, error.reason)
        }
        assertNull(ReaderLuaDataSource(loader).lastFile(source("return {}")))
    }

    @Test fun `oversize inputs and cancelled evaluation terminate`() = runBlocking {
        try {
            BoundedLuaLoader().read(
                ByteArrayInputStream(ByteArray(BoundedLuaLoader.MAX_BYTES + 1)),
            ) {
            }
            fail("Expected size limit")
        } catch (error: LuaSourceException) {
            assertEquals(LuaFailure.TooLarge, error.reason)
        }
        try {
            withTimeout(100) {
                BoundedLuaLoader(Int.MAX_VALUE).read(source("while true do end")) { }
            }
            fail("Expected cancellation")
        } catch (_: TimeoutCancellationException) { }
        assertNull(ReaderLuaDataSource().lastFile(source("return {}")))
    }

    @Test fun epubPagesUseFullPrecisionAndHandleBookBoundaries() = runTest {
        val reader = ReaderLuaDataSource()
        for ((percent, total, expected) in listOf(
            Triple(0.3549, 10000, 3549),
            Triple(0.123456, 100000, 12346),
            Triple(0.0, 416, 0),
            Triple(1.0, 416, 416),
        )) {
            val result = reader.sidecar(
                source("return {percent_finished=$percent,doc_pages=$total,last_xpointer='/body'}"),
            )
            assertEquals(SidecarDto(percent, null, expected, total), result)
        }
    }

    @Test fun missingOrInvalidTotalPagesPreserveProgressAndStatus() = runTest {
        val reader = ReaderLuaDataSource()
        val prefix = "percent_finished=0.35,summary={status='reading'},"
        for (pages in listOf(
            "",
            "doc_pages=0",
            "doc_pages=416.5",
            "doc_pages='416'",
            "doc_pages=2147483648",
            "doc_pages=0/0",
        )) {
            assertEquals(
                pages,
                SidecarDto(0.35, "reading"),
                reader.sidecar(source("return {" + prefix + pages + "}")),
            )
        }
        assertEquals(
            SidecarDto(null, null),
            reader.sidecar(source("return {doc_pages=416}")),
        )
    }

    @Test fun `sidecar percent and status are typed and bounded`() = runTest {
        val reader = ReaderLuaDataSource()
        assertEquals(
            SidecarDto(0.35, "reading"),
            reader.sidecar(source("return {percent_finished=0.35,summary={status='reading'}}")),
        )
        assertEquals(SidecarDto(null, null), reader.sidecar(source("return {}")))
        for (script in listOf(
            "return {percent_finished=1.5}",
            "return {percent_finished=0/0}",
            "return {summary={status=42}}",
        )) {
            try {
                reader.sidecar(source(script))
                fail("Invalid sidecar accepted")
            } catch (
                _: LuaSourceException,
            ) { }
        }
    }
}
