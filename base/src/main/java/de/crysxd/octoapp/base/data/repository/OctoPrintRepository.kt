package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.models.AppSettings
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.source.DataSource
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.octoprint.isBasedOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber

class OctoPrintRepository(
    private val dataSource: DataSource<List<OctoPrintInstanceInformationV3>>,
    private val octoPreferences: OctoPreferences,
    private val sensitiveDataMask: SensitiveDataMask,
) {

    private val instanceInformationFlow = MutableStateFlow<Map<String, OctoPrintInstanceInformationV3>>(emptyMap())
    private val lock = Mutex()

    init {
        // Upgrade active
        @Suppress("Deprecation")
        try {
            octoPreferences.activeInstanceWebUrl?.let { url ->
                val instance = getAll().first { it.isForWebUrl(url.toHttpUrl()) }
                octoPreferences.activeInstanceId = instance.id
                Timber.i("Upgrade active instance from $url to ${instance.id}")
            }
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            octoPreferences.activeInstanceWebUrl = null
        }

        postInstances()
    }

    fun instanceInformationFlow(instanceId: String? = null) = instanceInformationFlow.flatMapLatest { map ->
        if (instanceId == null) {
            octoPreferences.updatedFlow.map { octoPreferences.activeInstanceId }.distinctUntilChanged()
        } else {
            flowOf(instanceId)
        }.map { id ->
            id?.let { map[it] }
        }
    }

    fun getActiveInstanceSnapshot() = octoPreferences.activeInstanceId?.let { instanceInformationFlow.value[it] }

    private fun postInstances() {
        instanceInformationFlow.value = getAll().onEach {
            sensitiveDataMask.registerInstance(it)
        }.map {
            it.id to it
        }.toMap()

        Timber.i("Posting ${instanceInformationFlow.value.size} instances")
    }

    private fun storeOctoprintInstanceInformation(id: String, instance: OctoPrintInstanceInformationV3?) {
        Timber.i("Updating $id to $instance")
        val updated = getAll().filter { it.id != id }.toMutableList()
        instance?.let { updated.add(it) }
        dataSource.store(updated)
        postInstances()
    }

    fun setActive(instance: OctoPrintInstanceInformationV3) {
        Timber.i("Setting as active: ${instance.webUrl}")
        storeOctoprintInstanceInformation(instance.id, instance)
        octoPreferences.activeInstanceId = instance.id
    }

    suspend fun updateActive(block: suspend (OctoPrintInstanceInformationV3) -> OctoPrintInstanceInformationV3?) {
        getActiveInstanceSnapshot()?.let {
            update(it.id, block)
        }
    }

    suspend fun update(id: String, block: suspend (OctoPrintInstanceInformationV3) -> OctoPrintInstanceInformationV3?) {
        lock.withLock {
            get(id)?.let {
                val new = block(it)
                if (new != it) {
                    Timber.i("Updating instance with $id")
                    storeOctoprintInstanceInformation(it.id, new)
                } else {
                    Timber.v("Drop update, no changes")
                }
            }
        }
    }

    suspend fun updateAppSettingsForActive(block: suspend (AppSettings) -> AppSettings) {
        updateActive {
            it.copy(appSettings = block(it.appSettings ?: AppSettings()))
        }
    }

    fun clearActive() {
        Timber.i("Clearing active")
        octoPreferences.activeInstanceId = null
    }

    fun remove(id: String) {
        Timber.i("Removing $id")
        val all = getAll().filter { it.id != id }
        dataSource.store(all)
    }

    fun get(id: String) = dataSource.get()?.firstOrNull { it.id == id }

    fun getAll() = dataSource.get() ?: emptyList()

    fun findInstances(url: HttpUrl?) = getAll().mapNotNull {
        url ?: return@mapNotNull null

        val webUrl = it.webUrl
        val alternativeWebUrl = it.alternativeWebUrl
        when {
            url.isBasedOn(webUrl) -> it to false
            url.isBasedOn(alternativeWebUrl) -> it to true
            else -> null
        }
    }

    fun findInstancesWithWebUrl(url: HttpUrl?) = getAll().firstOrNull {
        url?.isBasedOn(it.webUrl) ?: false
    }
}