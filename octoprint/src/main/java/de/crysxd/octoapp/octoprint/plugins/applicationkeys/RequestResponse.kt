package de.crysxd.octoapp.octoprint.plugins.applicationkeys

import com.google.gson.annotations.SerializedName

data class RequestResponse(
    @SerializedName("app_token") val appToken: String
)