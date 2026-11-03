package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.InstalledAppsDataSource
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DeviceInstalledAppsRepository(
    private val dataSource: InstalledAppsDataSource,
    private val preferencesRepository: UserPreferencesRepository,
    applicationScope: CoroutineScope,
) : InstalledAppsRepository {
    private data class Catalog(
        val apps: List<AppInfo> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val catalog = MutableStateFlow(Catalog())
    private val reloadMutex = Mutex()
    override val installedApps = catalog.map { it.apps }.distinctUntilChanged()
    override val isLoading = catalog.map { it.loading }.distinctUntilChanged()
    override val error: Flow<String?> = combine(catalog, preferencesRepository.readError) {
            apps,
            prefsError,
        ->
        apps.error ?: prefsError
    }.distinctUntilChanged()

    override val pinnedApps: StateFlow<List<PinnedApp>> = combine(
        installedApps,
        preferencesRepository.pinnedComponentIds,
    ) { apps, pinnedIds ->
        val appMap = apps.associateBy(AppInfo::componentId)
        pinnedIds.mapNotNull { id ->
            appMap[id]?.let { PinnedApp(it.componentId, it.label, it.iconVersion) }
        }
    }.stateIn(applicationScope, SharingStarted.Eagerly, emptyList())

    init {
        applicationScope.launch { reload() }
    }

    override suspend fun reload(retainPackages: Set<String>) {
        preferencesRepository.retryRead()
        reloadMutex.withLock {
            catalog.value = catalog.value.copy(loading = true, error = null)
            try {
                val queried = dataSource.loadInstalledApps()
                // A replacing package can be absent from PackageManager temporarily.
                val retained = catalog.value.apps.filter {
                    it.componentId.substringBefore('/') in retainPackages
                }
                val apps = (queried + retained).distinctBy(AppInfo::componentId)
                catalog.value = Catalog(apps, loading = true)
                preferencesRepository.pruneStalePins(apps.map(AppInfo::componentId).toSet())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                catalog.value =
                    catalog.value.copy(error = "Unable to refresh apps. Please try again.")
            } finally {
                catalog.value = catalog.value.copy(loading = false)
            }
        }
    }

    override suspend fun pin(componentId: String) = preferencesRepository.pin(componentId)
    override suspend fun unpin(componentId: String) = preferencesRepository.unpin(componentId)
    override suspend fun movePin(componentId: String, offset: Int) =
        preferencesRepository.movePin(componentId, offset)
}
