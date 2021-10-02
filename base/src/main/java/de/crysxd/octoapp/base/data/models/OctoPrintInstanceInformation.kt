package de.crysxd.octoapp.base.data.models

import de.crysxd.octoapp.octoprint.UPNP_ADDRESS_PREFIX
import de.crysxd.octoapp.octoprint.isBasedOn
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.system.SystemCommand
import de.crysxd.octoapp.octoprint.models.system.SystemInfo
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID
import kotlin.math.max
import kotlin.reflect.KClass

private const val M115_MASK = "{m115 response}"

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
    // m115Response is only updated if Gcode Preview feature is enabled
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

    // We do not want to log the M115 response all over the place. It clutters the logs.
    override fun toString(): String = if (m115Response != null && m115Response != M115_MASK) {
        copy(m115Response = M115_MASK).toString()
    } else {
        super.toString()
    }
}

data class OctoPrintInstanceInformationV3(
    val id: String,
    val notificationId: Int? = null,
    val webUrl: HttpUrl,
    val alternativeWebUrl: HttpUrl? = null,
    val apiKey: String,
    val issue: ActiveInstanceIssue? = null,
    val m115Response: String? = null,
    val systemInfo: SystemInfo.Info? = null,
    val settings: Settings? = null,
    val activeProfile: PrinterProfiles.Profile? = null,
    val systemCommands: List<SystemCommand>? = null,
    val appSettings: AppSettings? = null,
    val octoEverywhereConnection: OctoEverywhereConnection? = null,
) {
    constructor(legacy: OctoPrintInstanceInformationV2) : this(
        id = UUID.randomUUID().toString(),
        webUrl = legacy.webUrl.toHttpUrl(),
        notificationId = null,
        alternativeWebUrl = legacy.alternativeWebUrl?.toHttpUrl(),
        apiKey = legacy.apiKey,
        issue = legacy.issue,
        m115Response = legacy.m115Response,
        settings = legacy.settings,
        activeProfile = legacy.activeProfile,
        systemCommands = legacy.systemCommands,
        appSettings = legacy.appSettings,
        octoEverywhereConnection = legacy.octoEverywhereConnection,
    )

    @Deprecated("Don't use this anymore")
    val isWebcamSupported
        get() = settings?.webcam?.webcamEnabled == true

    val label
        get() = settings?.appearance?.name?.takeIf {
            it.isNotBlank()
        } ?: webUrl.let { url ->
            val host = url.host
            val port = ":${url.port}".takeIf { HttpUrl.defaultPort(url.scheme) != url.port } ?: ""
            if (host.startsWith(UPNP_ADDRESS_PREFIX)) {
                val id = String.format("%x", host.hashCode()).take(3).uppercase()
                String.format("OctoPrint via UPnP ($id)")
            } else {
                "$host$port"
            }
        }

    fun isForWebUrl(webUrl: HttpUrl) = webUrl.isBasedOn(this.webUrl) || webUrl.isBasedOn(this.alternativeWebUrl)
}

fun OctoPrintInstanceInformationV3?.hasPlugin(plugin: KClass<out Settings.PluginSettings>) =
    this?.settings?.plugins?.any { it.value::class.java.isAssignableFrom(plugin.java) } == true

