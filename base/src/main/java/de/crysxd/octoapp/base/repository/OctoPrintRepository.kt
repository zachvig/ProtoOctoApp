package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.models.AppSettings
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class OctoPrintRepository(
    private val legacyDataSource: DataSource<OctoPrintInstanceInformationV2>,
    private val dataSource: DataSource<List<OctoPrintInstanceInformationV2>>,
    private val octoPreferences: OctoPreferences,
    private val sensitiveDataMask: SensitiveDataMask,
) {

    private val instanceInformationChannel = ConflatedBroadcastChannel<OctoPrintInstanceInformationV2?>(null)

    init {
        // Upgrade from legacy to new data source
        legacyDataSource.get()?.let {
            setActive(it)
            legacyDataSource.store(null)
        }

        postActiveInstance()
    }

    fun instanceInformationFlow() = instanceInformationChannel.asFlow().distinctUntilChanged()

    fun getActiveInstanceSnapshot() = instanceInformationChannel.valueOrNull

    private fun postActiveInstance() {
        val activeWebUrl = octoPreferences.activeInstanceWebUrl
        val activeInstance = getAll().firstOrNull {
            it.webUrl == activeWebUrl
        }
        activeInstance?.let {
            sensitiveDataMask.registerWebUrl(it.webUrl)
            sensitiveDataMask.registerApiKey(it.apiKey)
            it.settings?.webcam?.streamUrl?.let { url ->
                sensitiveDataMask.registerWebcamUrl(url)
            }
        }
        instanceInformationChannel.offer(activeInstance)
    }

    private fun storeOctoprintInstanceInformation(webUrl: String, instance: OctoPrintInstanceInformationV2?) {
        Timber.i("Updating $instance")
        val checked = if (instance == null || instance.webUrl.isBlank()) {
            null
        } else {
            instance
        }

        val updated = getAll().mapNotNull {
            if (it.webUrl == webUrl) {
                null
            } else {
                it
            }
        }.toMutableList()

        checked?.let {
            updated.add(it)
        }

        dataSource.store(updated)
        postActiveInstance()
    }

    fun setActive(instance: OctoPrintInstanceInformationV2) {
        storeOctoprintInstanceInformation(instance.webUrl, instance)
        octoPreferences.activeInstanceWebUrl = instance.webUrl
        Timber.i("Setting as active: ${instance.webUrl}")
        postActiveInstance()
    }

    suspend fun updateActive(block: suspend (OctoPrintInstanceInformationV2) -> OctoPrintInstanceInformationV2?) {
        instanceInformationChannel.valueOrNull?.let {
            storeOctoprintInstanceInformation(it.webUrl, block(it))
        }
    }

    suspend fun updateAppSettingsForActive(block: suspend (AppSettings) -> AppSettings) {
        updateActive {
            it.copy(appSettings = block(it.appSettings ?: AppSettings()))
        }
    }

    fun clearActive() {
        Timber.i("Clearing active")
        octoPreferences.activeInstanceWebUrl = null
        postActiveInstance()
    }

    fun remove(webUrl: String) {
        Timber.i("Removing $webUrl")
        val all = getAll().filter { it.webUrl != webUrl }
        dataSource.store(all)
    }

    suspend fun reportActiveApiKeyInvalid() = updateActive {
        it.copy(apiKey = "", apiKeyWasInvalid = true)
    }

    fun getAll() = dataSource.get() ?: emptyList()
}