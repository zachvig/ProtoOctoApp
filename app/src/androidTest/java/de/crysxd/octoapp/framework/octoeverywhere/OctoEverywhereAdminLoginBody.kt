package de.crysxd.octoapp.framework.octoeverywhere

import com.google.gson.annotations.SerializedName

data class OctoEverywhereAdminLoginBody(
    @SerializedName("AppleServerToken") val appleServerToken: String? = null,
    @SerializedName("Code") val code: String? = null,
    @SerializedName("Email") val email: String? = null,
    @SerializedName("FacebookAccessToken") val facebookAccessToken: String? = null,
    @SerializedName("GoogleServerToken") val googleServerToken: String? = null,
    @SerializedName("Password") val password: String? = null,
)