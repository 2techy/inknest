package dev.twotechy.inknest.data.source.lua

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.DebugLib

internal enum class LuaFailure { TooLarge, InstructionLimit, InvalidData }
internal class LuaSourceException(val reason: LuaFailure) : IOException(reason.name)

internal class BoundedLuaLoader(private val instructionLimit: Int = 5_000_000) {
    // Parser objects stay inside the data layer; each call owns a fresh VM and budget.
    suspend fun <T> read(input: InputStream, decode: (LuaTable) -> T): T {
        val script = readBytes(input)
        return withContext(Dispatchers.Default) {
            val context = currentCoroutineContext()
            val globals = Globals()
            // LuaJ 3.0.1 DebugLib initialization expects package.loaded. This is an empty
            // bootstrap table, removed before compilation; no package library is installed.
            globals.set("package", LuaTable().apply { set("loaded", LuaTable()) })
            var instructions = 0
            globals.load(object : DebugLib() {
                override fun onInstruction(pc: Int, v: Varargs, top: Int) {
                    if (++instructions > instructionLimit) throw Abort(null)
                    if (instructions % 1024 == 0) {
                        try {
                            context.ensureActive()
                        } catch (cancelled: CancellationException) {
                            throw Abort(cancelled)
                        }
                    }
                }
            })
            globals.set("debug", LuaValue.NIL)
            globals.set("package", LuaValue.NIL)
            LuaC.install(globals)
            try {
                val result = globals.load(script.toString(Charsets.UTF_8), "source").call()
                if (!result.istable()) throw LuaSourceException(LuaFailure.InvalidData)
                context.ensureActive()
                decode(result.checktable())
            } catch (abort: Abort) {
                abort.cancelled?.let { throw it }
                throw LuaSourceException(LuaFailure.InstructionLimit)
            } catch (_: LuaError) {
                throw LuaSourceException(LuaFailure.InvalidData)
            } catch (_: StackOverflowError) {
                throw LuaSourceException(LuaFailure.InvalidData)
            }
        }
    }

    private suspend fun readBytes(input: InputStream): ByteArray = withContext(Dispatchers.IO) {
        val context = currentCoroutineContext()
        val buffer = ByteArray(8192)
        val bytes = ByteArrayOutputStream()
        while (true) {
            context.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            if (bytes.size() + count > MAX_BYTES) throw LuaSourceException(LuaFailure.TooLarge)
            bytes.write(buffer, 0, count)
        }
        bytes.toByteArray()
    }

    private class Abort(val cancelled: CancellationException?) : Error()
    companion object {
        const val MAX_BYTES = 8 * 1024 * 1024
    }
}
