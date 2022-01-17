package de.crysxd.octoapp.octoprint.plugins.power.wemoswitch

import com.google.gson.annotations.SerializedName

data class WemoSwitchResponse(
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