package de.crysxd.octoapp.octoprint.models.profiles

import com.google.gson.annotations.SerializedName

data class PrinterProfiles(
    val profiles: Map<String, Profile>
) {

    data class Profile(
        val current: Boolean = false,
        val default: Boolean = false,
        val model: String = "fallback",
        val name: String = "Fallback",
        val volume: Volume = Volume(200f, 200f, 200f, PrinterProfiles.Origin.LowerLeft),
        val extruder: Extruder = Extruder(0.4f, 1 , false),
        val heatedChamber: Boolean = false,
        val heatedBed: Boolean = true,
    )

    data class Volume(
        val depth: Float,
        val width: Float,
        val height: Float,
        val origin: Origin,
    )

    data class Extruder(
        val nozzleDiameter: Float,
        val count: Int,
        val sharedNozzle: Boolean,
    )

    enum class Origin {
        @SerializedName("lowerleft")
        LowerLeft,

        @SerializedName("center")
        Center
    }
}