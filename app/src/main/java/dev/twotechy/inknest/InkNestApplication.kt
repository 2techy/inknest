package dev.twotechy.inknest

import android.app.Application
import dev.twotechy.inknest.di.createAppContainer

class InkNestApplication : Application() {
    internal val container by lazy { createAppContainer(this) }
}
