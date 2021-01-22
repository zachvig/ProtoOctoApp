package de.crysxd.octoapp.base.models

import de.crysxd.octoapp.octoprint.models.settings.Settings

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
    val apiKey: String,
    val apiKeyWasInvalid: Boolean = false,
    val m115Response: String? = null,
    val settings: Settings? = null,
    val appSettings: AppSettings? = null,
) {
    constructor(legacy: OctoPrintInstanceInformationV1) : this(
        webUrl = "http://${legacy.hostName}:${legacy.port}",
        apiKey = legacy.apiKey,
        settings = null,
        apiKeyWasInvalid = legacy.apiKeyWasInvalid
    )

    val isWebcamSupported get() = settings?.webcam?.webcamEnabled == true
}