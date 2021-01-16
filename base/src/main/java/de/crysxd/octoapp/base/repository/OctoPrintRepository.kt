package de.crysxd.octoapp.base.repository

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.datasource.DataSource
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
) {

    private val instanceInformationChannel = ConflatedBroadcastChannel<OctoPrintInstanceInformationV2?>(null)

    @Deprecated("Use instanceInformationFlow()")
    val instanceInformation = instanceInformationChannel.asFlow().asLiveData()

    init {
        // Upgrade from legacy to new data source
        legacyDataSource.get()?.let {
            setActiveInstance(it)
            legacyDataSource.store(null)
        }

        postActiveInstance()
    }

    fun instanceInformationFlow() = instanceInformationChannel.asFlow().distinctUntilChanged()

    fun setActiveInstance(info: OctoPrintInstanceInformationV2) {
        octoPreferences.activeInstanceWebUrl = info.webUrl
        storeOctoprintInstanceInformation(info.webUrl, info)
    }

    fun getActiveInstanceSnapshot() = instanceInformationChannel.valueOrNull

    private fun postActiveInstance() {
        val activeWebUrl = octoPreferences.activeInstanceWebUrl
        val activeInstance = getAll().firstOrNull {
            it.webUrl == activeWebUrl
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

    fun clearActive() {
        octoPreferences.activeInstanceWebUrl = null
        postActiveInstance()
    }

    suspend fun reportActiveApiKeyInvalid() = updateActive {
        it.copy(apiKey = "", apiKeyWasInvalid = true)
    }

    fun getAll() = dataSource.get() ?: emptyList()
}