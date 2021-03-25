package de.crysxd.octoapp.base.models

import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.system.SystemCommand
import kotlin.math.max

private const val M115_MASK = "{m155 response}"

data class OctoPrintInstanceInformationV1(
    val hostName: String,
    val port: Int,
    val apiKey: String,
    val supportsPsuPlugin: Boolean = false,
    val supportsWebcam: Boolean = false,
    val apiKeyWasInvalid: Boolean = false
)

data class OctoPrintInstanceInformationV2(
    val webUrl: String,
    val alternativeWebUrl: String? = null,
    val apiKey: String,
    val apiKeyWasInvalid: Boolean = false,
    val m115Response: String? = null,
    val settings: Settings? = null,
    val activeProfile: PrinterProfiles.Profile? = null,
    val systemCommands: List<SystemCommand>? = null,
    val appSettings: AppSettings? = null,
    val octoEverywhereConnection: OctoEverywhereConnection? = null,
) {
    constructor(legacy: OctoPrintInstanceInformationV1) : this(
        webUrl = "http://${legacy.hostName}:${legacy.port}",
        apiKey = legacy.apiKey,
        settings = null,
        apiKeyWasInvalid = legacy.apiKeyWasInvalid
    )

    val isWebcamSupported get() = settings?.webcam?.webcamEnabled == true

    val label
        get() = settings?.appearance?.name?.takeIf { it.isNotBlank() } ?: webUrl.let {
            val protocolEnd = it.indexOf("://") + 3
            val userInfoEnd = it.indexOf("@") + 1
            it.substring(max(protocolEnd, userInfoEnd))
        }

    // We do not want to log the M115 response all over the place. It clutters the logs.
    override fun toString(): String = if (m115Response != null && m115Response != M115_MASK) {
        copy(m115Response = M115_MASK).toString()
    } else {
        super.toString()
    }
}