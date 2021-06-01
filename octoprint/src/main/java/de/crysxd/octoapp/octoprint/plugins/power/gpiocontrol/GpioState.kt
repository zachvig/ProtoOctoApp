package de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol

import com.google.gson.annotations.SerializedName

enum class GpioState {
    @SerializedName("on")
    ON,

    @SerializedName("off")
    OFF
}