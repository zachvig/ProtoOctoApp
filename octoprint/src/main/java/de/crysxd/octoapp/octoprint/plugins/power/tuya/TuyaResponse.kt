package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName

data class TuyaResponse(
    val currentState: State?
) {

    enum class State {
        @SerializedName("on")
        ON,

        @SerializedName("off")
        OFF,

        @SerializedName("unknown")
        UNKNOWN
    }
}