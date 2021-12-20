package de.crysxd.octoapp.octoprint.models.timelapse

data class TimelapseStatus(
    val config: TimelapseConfig?,
    val enabled: Boolean?,
    val files: List<TimelapseFile>?,
    val unrendered: List<TimelapseFile>?,
)