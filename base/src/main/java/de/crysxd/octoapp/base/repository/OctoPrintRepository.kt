package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.models.ActiveInstanceIssue
import de.crysxd.octoapp.base.models.AppSettings
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.octoprint.isBasedOn
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import java.lang.Exception

@Suppress("EXPERIMENTAL_API_USAGE")
class OctoPrintRepository(
    private val dataSource: DataSource<List<OctoPrintInstanceInformationV3>>,
    private val octoPreferences: OctoPreferences,
    private val sensitiveDataMask: SensitiveDataMask,
) {

    private val instanceInformationChannel = ConflatedBroadcastChannel<OctoPrintInstanceInformationV3?>(null)

    init {
        // Upgrade active
        @Suppress("Deprecation")
        try {
            octoPreferences.activeInstanceWebUrl?.let { url ->
                val instance = getAll().first { it.isForWebUrl(url.toHttpUrl()) }
                octoPreferences.activeInstanceId = instance.id
            }
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            octoPreferences.activeInstanceWebUrl = null
        }

        postActiveInstance()
    }

    fun instanceInformationFlow() = instanceInformationChannel.asFlow().distinctUntilChanged()

    fun getActiveInstanceSnapshot() = instanceInformationChannel.valueOrNull

    private fun postActiveInstance() {
        val activeInstance = octoPreferences.activeInstanceId?.let(::get)
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

    private fun storeOctoprintInstanceInformation(id: String, instance: OctoPrintInstanceInformationV3?) {
        Timber.i("Updating $id to $instance")
        val updated = getAll().filter { it.id != id }.toMutableList()
        instance?.let { updated.add(it) }
        dataSource.store(updated)
        postActiveInstance()
    }

    fun setActive(instance: OctoPrintInstanceInformationV3) {
        storeOctoprintInstanceInformation(instance.id, instance)
        octoPreferences.activeInstanceId = instance.id
        Timber.i("Setting as active: ${instance.webUrl}")
        postActiveInstance()
    }

    suspend fun updateActive(block: suspend (OctoPrintInstanceInformationV3) -> OctoPrintInstanceInformationV3?) {
        instanceInformationChannel.valueOrNull?.let {
            storeOctoprintInstanceInformation(it.id, block(it))
        }
    }

    fun update(id: String, block: (OctoPrintInstanceInformationV3) -> OctoPrintInstanceInformationV3?) {
        get(id)?.let {
            storeOctoprintInstanceInformation(it.id, block(it))
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
        postActiveInstance()
    }

    fun remove(id: String) {
        Timber.i("Removing $id")
        val all = getAll().filter {it.id != id }
        dataSource.store(all)
    }

    suspend fun reportIssueWithActiveInstance(issue: ActiveInstanceIssue) {
        Timber.w("Issue reported with")
        updateActive {
            it.copy(apiKey = it.apiKey.takeUnless { issue == ActiveInstanceIssue.INVALID_API_KEY } ?: "", issue = issue)
        }
    }

    fun get(id: String) = dataSource.get()?.firstOrNull { it.id == id }

    fun getAll() = dataSource.get() ?: emptyList()

    fun findInstances(url: HttpUrl) = getAll().mapNotNull {
        val webUrl = it.webUrl
        val alternativeWebUrl = it.alternativeWebUrl
        when {
            url.isBasedOn(webUrl) -> it to false
            url.isBasedOn(alternativeWebUrl) -> it to true
            else -> null
        }
    }
}