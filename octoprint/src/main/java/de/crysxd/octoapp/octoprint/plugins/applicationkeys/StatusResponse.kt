package de.crysxd.octoapp.octoprint.plugins.applicationkeys

import com.google.gson.annotations.SerializedName

data class CheckResponse(
    @SerializedName("api_key") val apiKey: String
)