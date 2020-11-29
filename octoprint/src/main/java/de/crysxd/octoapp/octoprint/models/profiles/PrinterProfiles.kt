package de.crysxd.octoapp.octoprint.models.profiles

import com.google.gson.annotations.SerializedName

data class PrinterProfiles(
    val profiles: Map<String, Profile>
) {

    data class Profile(
        val current: Boolean,
        val default: Boolean,
        val model: String,
        val name: String,
        val volume: Volume,
        val extruder: Extruder
    )

    data class Volume(
        val depth: Float,
        val width: Float,
        val height: Float,
        val origin: Origin,
    )

    data class Extruder(
        val nozzleDiameter: Float
    )

    enum class Origin {
        @SerializedName("lowerleft")
        LowerLeft,

        @SerializedName("center")
        Center
    }
}