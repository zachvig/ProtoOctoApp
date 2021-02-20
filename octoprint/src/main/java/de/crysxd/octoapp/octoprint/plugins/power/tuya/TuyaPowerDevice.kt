package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class TuyaPowerDevice(
    override val id: String,
    @Transient val plugin: TuyaPowerPlugin?,
    @SerializedName("label") override val displayName: String,
) : PowerDevice() {

    @Transient
    override val pluginDisplayName = "Tuya"

    @Transient
    override val pluginId = "tuyasmartplug"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from TuyaPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from TuyaPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from TuyaPowerPlugin!")

}