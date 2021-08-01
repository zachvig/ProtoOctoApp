package de.crysxd.octoapp.base.models

import de.crysxd.octoapp.base.network.OctoPrintUpnpDiscovery.Companion.UPNP_ADDRESS_PREFIX
import de.crysxd.octoapp.octoprint.extractAndRemoveUserInfo
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
    val issue: ActiveInstanceIssue? = null,
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
    )

    val isWebcamSupported get() = settings?.webcam?.webcamEnabled == true

    val label
        get() = settings?.appearance?.name?.takeIf {
            it.isNotBlank()
        } ?: webUrl.let {
            val protocolEnd = it.indexOf("://") + 3
            val userInfoEnd = it.indexOf("@") + 1
            val host = it.substring(max(protocolEnd, userInfoEnd)).removeSuffix("/")
            if (host.startsWith(UPNP_ADDRESS_PREFIX)) {
                val id = String.format("%x", host.hashCode()).take(3).uppercase()
                String.format("OctoPrint via UPnP ($id)")
            } else {
                host
            }
        }

    // The URL contains the Basic Auth, if the user changes the basic auth the url does not exactly match but it references the same instance
    fun isSameInstanceAs(other: OctoPrintInstanceInformationV2) = webUrl.extractAndRemoveUserInfo().first == other.webUrl.extractAndRemoveUserInfo().first
    fun isForWebUrl(webUrl: String) = this.webUrl.extractAndRemoveUserInfo().first == webUrl.extractAndRemoveUserInfo().first

    // We do not want to log the M115 response all over the place. It clutters the logs.
    override fun toString(): String = if (m115Response != null && m115Response != M115_MASK) {
        copy(m115Response = M115_MASK).toString()
    } else {
        super.toString()
    }
}