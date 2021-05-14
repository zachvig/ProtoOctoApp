package de.crysxd.octoapp.octoprint.plugins.power.ws281x

import com.google.gson.annotations.SerializedName

data class WS281xResponse(
    @SerializedName("lights_on")
    val lightsOn: Boolean?,
    @SerializedName("torch_on")
    val torchOn: Boolean?,
)