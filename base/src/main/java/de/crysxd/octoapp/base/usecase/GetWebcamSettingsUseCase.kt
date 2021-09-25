package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.isHlsStreamUrl
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.octoprint.resolvePath
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
) : UseCase<OctoPrintInstanceInformationV3?, List<WebcamSettings>?>() {

    override suspend fun doExecute(param: OctoPrintInstanceInformationV3?, timber: Timber.Tree): List<WebcamSettings> {
        // This is a little complicated on first glance, but not that bad
        //
        // Case A: A instance information is given via params (used for widgets) and we need to create an ad hoc instance
        //         In this case we need to fetch the settings to see wether we have a local or a remote connection. OctoEverywhere also will alter the settings
        //         with updated webcam URLs which we need
        //
        // Case B: No instance information is given in params and we use the "default" OctoPrint. In this case we need to check
        //         if the websocket is connected. If this is the case, we can rely on the isAlternativeUrlBeingUsed being correct, no additional network request
        //         is needed to test out the route
        val (octoPrint, settings, useAlternative) = param?.let {
            val o = octoPrintProvider.createAdHocOctoPrint(param)
            val s = o.createSettingsApi().getSettings()
            timber.i("Using ad-hoc connection (useAlternative=${o.isAlternativeUrlBeingUsed})")
            Triple(o, s, o.isAlternativeUrlBeingUsed)
        } ?: let {
            val o = octoPrintProvider.octoPrint()
            val connection = octoPrintProvider.passiveConnectionEventFlow("webcam-settings").firstOrNull()?.connectionType
            val s = octoPrintRepository.getActiveInstanceSnapshot()?.settings?.takeIf { connection != null } ?: o.createSettingsApi().getSettings()
            val a = (connection != null && connection != ConnectionType.Primary) || o.isAlternativeUrlBeingUsed
            timber.i("Using default connection (connection=$connection, useAlternative=$a)")
            Triple(o, s, a)
        }

        // Add all webcams from multicam plugin
        val webcamSettings = mutableListOf<WebcamSettings>()
        (settings.plugins.values.mapNotNull { it as? Settings.MultiCamSettings }.firstOrNull())?.profiles?.let { webcamSettings.addAll(it) }

        // If no webcams were added, use default settings object
        if (webcamSettings.isEmpty()) {
            webcamSettings.add(settings.webcam)
        }

        val primaryUrl = octoPrint.fullWebUrl
        val alternativeUrl = octoPrint.fullAlternativeWebUrl

        val newSettings = webcamSettings.mapNotNull { ws ->
            val streamUrl = ws.streamUrl ?: return@mapNotNull null

            val upgradedUrl = ws.streamUrl?.toHttpUrlOrNull() ?: let {
                // Conversion to HTTP url failed, indicating this URL is not absolute. Resolve from base.
                val base = alternativeUrl?.takeIf { useAlternative } ?: primaryUrl
                base.resolvePath(streamUrl)
            }

            ws.copy(
                multiCamUrl = upgradedUrl.toString(),
                absoluteStreamUrl = upgradedUrl,
                standardStreamUrl = upgradedUrl.toString(),
            )
        }

        OctoAnalytics.setUserProperty(
            OctoAnalytics.UserProperty.WebCamAvailable,
            when {
                newSettings.any { it.absoluteStreamUrl?.isHlsStreamUrl() == true } -> "hls"
                newSettings.any { it.absoluteStreamUrl != null } -> "mjpeg"
                else -> "false"
            }
        )

        return newSettings
    }
}
