package de.crysxd.octoapp.octoprint.plugins.power.wemoswitch

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class WemoSwitchPowerDevice(
    @SerializedName("ip") override val id: String,
    @Transient val plugin: WemoSwitchPowerPlugin?,
    @SerializedName("label") override val displayName: String,
) : PowerDevice() {
    override val capabilities
        get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)

    @Transient
    override val pluginDisplayName = "Wemo"

    @Transient
    override val pluginId = "wemo"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from WemoPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from WemoPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from WemoPowerPlugin!")

}