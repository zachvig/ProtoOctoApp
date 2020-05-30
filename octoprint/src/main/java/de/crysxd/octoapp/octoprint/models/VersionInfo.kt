package de.crysxd.octoapp.octoprint.models

import com.google.gson.annotations.SerializedName

data class VersionInfo(
    @SerializedName("api") val apiVersion: String,
    @SerializedName("server") val severVersion: String,
    @SerializedName("text") val serverVersionText: String
)