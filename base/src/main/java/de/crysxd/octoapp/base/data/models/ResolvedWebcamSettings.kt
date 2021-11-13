package de.crysxd.octoapp.base.data.models

import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import okhttp3.HttpUrl

sealed class ResolvedWebcamSettings(val urlString: String) {
    abstract val webcamSettings: WebcamSettings

    data class MjpegSettings(val url: HttpUrl, override val webcamSettings: WebcamSettings) : ResolvedWebcamSettings(url.toString())
    data class HlsSettings(val url: HttpUrl, override val webcamSettings: WebcamSettings, val basicAuth: String?) : ResolvedWebcamSettings(url.toString())
    data class RtspSettings(val url: String, override val webcamSettings: WebcamSettings, val basicAuth: String?) : ResolvedWebcamSettings(url)
}
