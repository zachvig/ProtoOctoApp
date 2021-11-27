package de.crysxd.octoapp.octoprint.plugins.thespaghettidetective

import com.google.gson.annotations.SerializedName

data class SpaghettiDetectiveDataUsage(
    @SerializedName("monthly_cap") val monthlyCapBytes: Int,
    @SerializedName("reset_in_seconds") val resetInSeconds: Double,
    @SerializedName("total") val totalBytes: Int
)