package de.crysxd.octoapp.base.models

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
    val supportsPsuPlugin: Boolean = false,
    val supportsWebcam: Boolean = false,
    val apiKeyWasInvalid: Boolean = false
) {
    constructor(legacy: OctoPrintInstanceInformationV1) : this(
        webUrl = "http://${legacy.hostName}:${legacy.port}",
        apiKey = legacy.apiKey,
        supportsPsuPlugin = legacy.supportsPsuPlugin,
        supportsWebcam = legacy.supportsWebcam,
        apiKeyWasInvalid = legacy.apiKeyWasInvalid
    )
}