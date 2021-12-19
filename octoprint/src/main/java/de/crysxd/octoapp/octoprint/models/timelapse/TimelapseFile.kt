package de.crysxd.octoapp.octoprint.models.timelapse

data class TimelapseFile(
    val name: String?,
    val url: String?,
    val date: String?,
    val size: String?,
    val bytes: Long?,
    val processing: Boolean?,
    val rendering: Boolean?,
    val recording: Boolean?,
)