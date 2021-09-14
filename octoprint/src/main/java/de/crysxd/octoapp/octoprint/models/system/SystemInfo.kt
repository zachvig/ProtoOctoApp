package de.crysxd.octoapp.octoprint.models.system

import com.google.gson.annotations.SerializedName

data class SystemInfo(
    @SerializedName("systeminfo")
    val systemInfo: Info
) {
    data class Info(
        @SerializedName("printer.firmware")
        val printerFirmware: String?,
    )
}