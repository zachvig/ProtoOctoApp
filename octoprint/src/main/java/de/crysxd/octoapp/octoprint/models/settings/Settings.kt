package de.crysxd.octoapp.octoprint.models.settings

data class Settings(
    val webcam: WebcamSettings,
    val plugins: Map<String, Map<String, *>>
)