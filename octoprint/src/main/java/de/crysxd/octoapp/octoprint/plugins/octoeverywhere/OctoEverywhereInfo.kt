package de.crysxd.octoapp.octoprint.plugins.octoeverywhere

import com.google.gson.annotations.SerializedName

data class OctoEverywhereInfo(
    @SerializedName("PluginVersion") val version: String,
    @SerializedName("PrinterId") val printerId: String,
)