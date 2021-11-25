package de.crysxd.octoapp.octoprint.plugins.thespaghettidetective

import com.google.gson.annotations.SerializedName

data class SpaghettiDetectiveStatus(
    @SerializedName("linked_printer") val linkedPrinter: LinkedPrinter?
) {
    data class LinkedPrinter(
        @SerializedName("id") val id: String?,
        @SerializedName("is_pro") val isPro: Boolean?,
        @SerializedName("name") val name: String?,
    )
}