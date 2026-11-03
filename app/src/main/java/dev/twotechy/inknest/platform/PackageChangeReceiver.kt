package dev.twotechy.inknest.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PackageChangeReceiver(
    private val repository: InstalledAppsRepository,
    private val scope: CoroutineScope,
    private val onIconsChanged: () -> Unit,
) : BroadcastReceiver() {
    private val replacingPackages = Collections.synchronizedSet(mutableSetOf<String>())
    private var debounceJob: Job? = null
    private var registeredContext: Context? = null

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val packageName = intent.data?.schemeSpecificPart
        if (action == Intent.ACTION_PACKAGE_REMOVED &&
            intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        ) {
            packageName?.let(replacingPackages::add)
            return
        }
        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            packageName?.let(replacingPackages::remove)
        }
        onIconsChanged()
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(300)
            repository.reload(synchronized(replacingPackages) { replacingPackages.toSet() })
        }
    }

    fun register(context: Context) {
        if (registeredContext != null) return
        val appContext = context.applicationContext
        val packages = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            appContext,
            this,
            packages,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            appContext,
            this,
            IntentFilter(Intent.ACTION_LOCALE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registeredContext = appContext
    }

    fun unregister() {
        debounceJob?.cancel()
        registeredContext?.unregisterReceiver(this)
        registeredContext = null
        replacingPackages.clear()
    }
}
