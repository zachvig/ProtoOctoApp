package de.crysxd.octoapp.notification

import com.google.gson.annotations.SerializedName

data class FcmPrintEvent(
    val fileDate: Long?,
    val fileName: String?,
    val progress: Float?,
    val type: Type,
    val instanceId: String = "",
    val timeLeft: Long?,
) {
    enum class Type {
        @SerializedName("printing")
        Printing,

        @SerializedName("paused")
        Paused,

        @SerializedName("completed")
        Completed,

        @SerializedName("filament_required")
        FilamentRequired,

        @SerializedName("idle")
        Idle,
    }
}

