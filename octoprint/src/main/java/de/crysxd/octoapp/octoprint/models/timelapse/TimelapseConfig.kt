package de.crysxd.octoapp.octoprint.models.timelapse

import com.google.gson.annotations.SerializedName

data class TimelapseConfig(
    val type: Type?,
    val fps: Int?,
    val postRoll: Int?,
    val minDelay: Int?,
    val interval: Int?,
    val retractionZHop: Float?,
) {
    enum class Type {
        @SerializedName("off")
        Off,

        @SerializedName("timed")
        Timed,

        @SerializedName("zchange")
        ZChange
    }
}