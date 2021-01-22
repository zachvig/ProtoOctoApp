package de.crysxd.octoapp.base.models

data class AppSettings(
    val webcamScaleType: Int? = null,
    val webcamFullscreenScaleType: Int? = null,
    val defaultPowerDevices: Map<String, String>? = null
)