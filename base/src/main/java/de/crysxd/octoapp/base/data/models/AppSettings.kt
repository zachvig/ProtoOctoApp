package de.crysxd.octoapp.base.data.models

import de.crysxd.octoapp.octoprint.models.settings.Settings

data class AppSettings(
    val webcamScaleType: Int? = null,
    val webcamFullscreenScaleType: Int? = null,
    val activeWebcamIndex: Int = 0,
    val defaultPowerDevices: Map<String, String>? = null,
    val selectedTerminalFilters: List<Settings.TerminalFilter>? = null,
    val isStyledTerminal: Boolean? = null,
    val moveZFeedRate: Int? = null
)