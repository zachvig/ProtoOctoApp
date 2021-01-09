package de.crysxd.octoapp.octoprint.plugins.power.tplinkplug

import com.google.gson.annotations.SerializedName

data class TpLinkSmartPlugResponse(
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