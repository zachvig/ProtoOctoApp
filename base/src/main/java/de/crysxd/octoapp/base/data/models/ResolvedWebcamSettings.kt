package de.crysxd.octoapp.base.data.models

import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import okhttp3.HttpUrl

sealed class ResolvedWebcamSettings(val description: String) {
    abstract val webcamSettings: WebcamSettings

    data class MjpegSettings(val url: HttpUrl, override val webcamSettings: WebcamSettings) : ResolvedWebcamSettings(url.toString())
    data class HlsSettings(val url: HttpUrl, override val webcamSettings: WebcamSettings, val basicAuth: String?) : ResolvedWebcamSettings(url.toString())
    data class RtspSettings(val url: String, override val webcamSettings: WebcamSettings, val basicAuth: String?) : ResolvedWebcamSettings(url)
    data class SpaghettiCamSettings(val webcamIndex: Int, override val webcamSettings: WebcamSettings) :
        ResolvedWebcamSettings("Spaghetti Detective Camera${" #${webcamIndex + 1}".takeIf { webcamIndex > 0 } ?: ""}")
}
