package de.crysxd.octoapp.octoprint.plugins.power.tplinkplug

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class TpLinkSmartPlugPowerDevice(
    @SerializedName("ip") override val id: String,
    @Transient val plugin: TpLinkSmartPlugPowerPlugin?,
    @SerializedName("label") override val displayName: String,
) : PowerDevice() {
    override val capabilities
        get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)

    @Transient
    override val pluginDisplayName = "TP-Link Plug"

    @Transient
    override val pluginId = "tplinksmartplug"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from TpLinkPlugPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from TpLinkPlugPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from TpLinkPlugPowerPlugin!")

}