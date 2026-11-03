package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceFileReader
import dev.twotechy.inknest.data.source.lua.LuaFailure
import dev.twotechy.inknest.data.source.lua.LuaSourceException
import dev.twotechy.inknest.data.source.lua.ReaderLuaDataSource
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ReaderProbeRepository(
    private val files: SourceFileReader,
    private val lua: ReaderLuaDataSource = ReaderLuaDataSource(),
) {
    private val mutex = Mutex()
    suspend fun probe(config: ReadingSourceConfig): ReaderProbeResult = mutex.withLock {
        ReaderProbeResult(
            current = capability(config.koreaderTreeUri) { tree ->
                files.read(tree, listOf("settings.reader.lua"), lua::lastFile)
            },
            history = capability(config.koreaderTreeUri) { tree ->
                files.read(tree, listOf("history.lua"), lua::history)
            },
        )
    }

    private suspend fun <T> capability(
        tree: String?,
        block: suspend (String) -> T,
    ): CapabilityResult<T> {
        val result = if (tree == null) {
            ProbeRead.Failed(ProbeIssue.NotConfigured)
        } else {
            try {
                ProbeRead.Success(block(tree))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: MissingSourceFile) {
                ProbeRead.Failed(ProbeIssue.MissingFile)
            } catch (_: SecurityException) {
                ProbeRead.Failed(ProbeIssue.PermissionLost)
            } catch (error: LuaSourceException) {
                ProbeRead.Failed(
                    when (error.reason) {
                        LuaFailure.TooLarge -> ProbeIssue.TooLarge
                        LuaFailure.InstructionLimit -> ProbeIssue.BudgetExceeded
                        LuaFailure.InvalidData -> ProbeIssue.InvalidLua
                    },
                )
            } catch (_: IOException) {
                ProbeRead.Failed(ProbeIssue.ProviderError)
            }
        }
        return CapabilityResult(result, System.currentTimeMillis())
    }
}
