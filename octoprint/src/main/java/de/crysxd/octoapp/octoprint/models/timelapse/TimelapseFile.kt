package de.crysxd.octoapp.octoprint.models.timelapse

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

data class TimelapseFile(
    val name: String?,
    val url: String?,
    val date: String?,
    val size: String?,
    val bytes: Long?,
    val processing: Boolean?,
    val rendering: Boolean?,
    val recording: Boolean?,
) : Serializable {
    companion object {
        private val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    }

    val unixDate
        get() = try {
            format.parse(date)
        } catch (e: Exception) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Unable to parse date", e)
            null
        }
}