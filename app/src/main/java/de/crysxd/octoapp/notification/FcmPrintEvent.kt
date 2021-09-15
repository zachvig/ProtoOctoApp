package de.crysxd.octoapp.notification

import com.google.gson.annotations.SerializedName

data class FcmPrintEvent(
    val fileName: String?,
    val progress: Float?,
    val type: Type,
    val serverTime: Long?,
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

