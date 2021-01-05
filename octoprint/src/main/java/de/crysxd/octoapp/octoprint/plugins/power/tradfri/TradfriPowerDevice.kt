package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class TradfriPowerDevice(
    val id: String,
    @Transient val plugin: TradfriPowerPlugin?,
    @SerializedName("name") override val displayName: String,
) : PowerDevice() {

    @Transient
    override val pluginDisplayName = "Trådfri"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

}