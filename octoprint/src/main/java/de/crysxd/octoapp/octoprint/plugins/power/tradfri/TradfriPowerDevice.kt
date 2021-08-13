package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class TradfriPowerDevice(
    @SerializedName("id") val idInt: Int,
    @Transient val plugin: TradfriPowerPlugin?,
    @SerializedName("name") override val displayName: String,
) : PowerDevice() {
    override val capabilities
        get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)

    override val id: String get() = idInt.toString()

    @Transient
    override val pluginDisplayName = "Trådfri"

    @Transient
    override val pluginId = "tradfri"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

}