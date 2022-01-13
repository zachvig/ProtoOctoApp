package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.models.ResolvedWebcamSettings
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.extractAndRemoveBasicAuth
import de.crysxd.octoapp.octoprint.isHlsStreamUrl
import de.crysxd.octoapp.octoprint.isSpaghettiDetectiveUrl
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.octoprint.resolvePath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
    private val getActiveHttpUrlUseCase: GetActiveHttpUrlUseCase,
) : UseCase<OctoPrintInstanceInformationV3?, Flow<List<ResolvedWebcamSettings>>>() {

    override suspend fun doExecute(param: OctoPrintInstanceInformationV3?, timber: Timber.Tree): Flow<List<ResolvedWebcamSettings>> {
        val activeWebUrlFlow = getActiveHttpUrlUseCase.execute(param)

        // If we use the active instance, consume the settings flow. Otherwise use the snapshot we have.
        val settingsFlow = if (param == null) {
            octoPrintRepository.instanceInformationFlow().map { it?.settings ?: octoPrintProvider.octoPrint().createSettingsApi().getSettings() }
        } else {
            flowOf(octoPrintProvider.createAdHocOctoPrint(param).createSettingsApi().getSettings())
        }.distinctUntilChangedBy { it.webcam.hashCode() + it.plugins.values.firstOrNull { it is Settings.MultiCamSettings }.hashCode() }

        // Compile webcam settings
        return activeWebUrlFlow.distinctUntilChanged().combine(settingsFlow) { activeWebUrl, settings ->
            Timber.d("Compiling webcam settings for $activeWebUrl")

            // Add all webcams from multicam plugin
            val webcamSettings = mutableListOf<WebcamSettings>()
            (settings.plugins.values.mapNotNull { it as? Settings.MultiCamSettings }.firstOrNull())?.profiles?.let { webcamSettings.addAll(it) }

            // If no webcams were added, use default settings object
            if (webcamSettings.isEmpty()) {
                webcamSettings.add(settings.webcam)
            }

            // If active URL is Spaghetti Tunnel, we need to use the dedicated Spaghetti stack
            val newSettings = when {
                activeWebUrl.isSpaghettiDetectiveUrl() -> buildSpaghettiSettings(webcamSettings)
                else -> buildSettings(webcamSettings, activeWebUrl)
            }

            Timber.d("Settings: $newSettings")
            newSettings
        }
    }

    private fun buildSpaghettiSettings(webcamSettings: List<WebcamSettings>) = webcamSettings.mapIndexed { index, ws ->
        ResolvedWebcamSettings.SpaghettiCamSettings(
            webcamSettings = ws,
            webcamIndex = index,
        )
    }

    private fun buildSettings(webcamSettings: List<WebcamSettings>, activeWebUrl: HttpUrl): List<ResolvedWebcamSettings> {
        val newSettings = webcamSettings.mapNotNull { ws ->
            fun HttpUrl.toWebcamSettings() = extractAndRemoveBasicAuth().let {
                if (it.first.isHlsStreamUrl()) {
                    ResolvedWebcamSettings.HlsSettings(url = it.first, webcamSettings = ws, basicAuth = it.second)
                } else {
                    ResolvedWebcamSettings.MjpegSettings(url = this, webcamSettings = ws)
                }
            }

            try {
                val streamUrl = ws.streamUrl ?: return@mapNotNull null
                when {
                    // Stream URL
                    ws.streamUrl?.toHttpUrlOrNull() != null -> ws.streamUrl!!.toHttpUrl().toWebcamSettings()

                    // RTSP URL
                    ws.streamUrl?.startsWith("rtsp://") == true -> {
                        val originalPrefix = "rtsp://"
                        val fakePrefix = "http://"
                        val fakeHttpUrl = (fakePrefix + ws.streamUrl!!.removePrefix(originalPrefix)).toHttpUrl()
                        val (url, basicAuth) = fakeHttpUrl.extractAndRemoveBasicAuth()
                        ResolvedWebcamSettings.RtspSettings(
                            url = originalPrefix + url.toString().removePrefix(fakePrefix),
                            webcamSettings = ws,
                            basicAuth = basicAuth
                        )
                    }

                    // No HTTP/S, no RTSP. Let's assume it's a HTTP path and resolve it
                    else -> activeWebUrl.resolvePath(streamUrl).toWebcamSettings()
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }

        OctoAnalytics.setUserProperty(
            OctoAnalytics.UserProperty.WebCamAvailable,
            when {
                newSettings.any { it is ResolvedWebcamSettings.RtspSettings } -> "rtsp"
                newSettings.any { it is ResolvedWebcamSettings.HlsSettings } -> "hls"
                newSettings.any { it is ResolvedWebcamSettings.MjpegSettings } -> "mjpeg"
                else -> "false"
            }
        )

        return newSettings
    }
}
