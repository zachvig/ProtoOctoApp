package de.crysxd.octoapp.octoprint.plugins.power.tasmota

import com.google.gson.annotations.SerializedName

data class TasmotaResponse(
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