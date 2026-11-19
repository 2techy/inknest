package dev.twotechy.inknest.data.source.koreader.lua

import java.io.IOException

internal class LuaSourceException(val reason: LuaFailure) : IOException(reason.name)
