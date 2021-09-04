package de.crysxd.octoapp.octoprint.models.settings

import com.google.gson.annotations.SerializedName

data class WebcamSettings(
    @SerializedName("streamUrl") private val standardStreamUrl: String?,
    @SerializedName("URL") private val multiCamUrl: String?,
    val flipH: Boolean,
    val flipV: Boolean,
    val rotate90: Boolean,
    val webcamEnabled: Boolean?,
    val streamRatio: String?,
    val authHeader: String?,
) {

    val saveStreamRatio get() = streamRatio ?: "16:9"
    val streamUrl get() = standardStreamUrl ?: multiCamUrl
}