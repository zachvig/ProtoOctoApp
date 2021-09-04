package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName

data class WledResponse(
    @SerializedName("lights_on") val lightsOn: Boolean?
)