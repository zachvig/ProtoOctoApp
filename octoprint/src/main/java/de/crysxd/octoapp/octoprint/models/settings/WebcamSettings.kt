package de.crysxd.octoapp.octoprint.models.settings

data class WebcamSettings(
    val streamUrl: String,
    val flipH: Boolean,
    val flipV: Boolean,
    val rotate90: Boolean,
    val webcamEnabled: Boolean,
    val streamRatio: String
)