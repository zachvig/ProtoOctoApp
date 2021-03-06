package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.models.ActiveInstanceIssue
import de.crysxd.octoapp.base.models.AppSettings
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.octoprint.models.settings.Settings
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
        val activeInstance = findOrNull(activeWebUrl)
        activeInstance?.let { instance ->
            sensitiveDataMask.registerWebUrl(instance.webUrl, "octoprint")
            sensitiveDataMask.registerWebUrl(instance.alternativeWebUrl, "alternative")
            sensitiveDataMask.registerWebUrl(instance.settings?.webcam?.streamUrl, "webcam")
            sensitiveDataMask.registerApiKey(instance.apiKey)
            instance.settings?.plugins?.values?.mapNotNull { it as? Settings.MultiCamSettings }?.firstOrNull()?.profiles?.forEachIndexed { i, webcam ->
                sensitiveDataMask.registerWebUrl(webcam.streamUrl, "webcam_$i")
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
            if (it.isForWebUrl(webUrl)) {
                null
            } else {
                it
            }
        }.toMutableList()

        checked?.let { updated.add(it) }
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
        val all = getAll().filter { !it.isForWebUrl(webUrl) }
        dataSource.store(all)
    }

    suspend fun reportIssueWithActiveInstance(issue: ActiveInstanceIssue) {
        Timber.w("Issue reported with")
        updateActive {
            it.copy(apiKey = it.apiKey.takeUnless { issue == ActiveInstanceIssue.INVALID_API_KEY } ?: "", issue = issue)
        }
    }

    fun getAll() = dataSource.get() ?: emptyList()

    fun findOrNull(webUrl: String?) = webUrl?.let {
        getAll().firstOrNull { it.isForWebUrl(webUrl) }
    }
}