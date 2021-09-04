package de.crysxd.octoapp.octoprint.plugins.power.wled

import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class WledPowerDevice(
    override val id: String = "wleds",
    @Transient val plugin: WledPowerPlugin?,
    override val displayName: String = "WLEDs",
) : PowerDevice() {

    override val capabilities
        get() = listOf(Capability.Illuminate)

    @Transient
    override val pluginDisplayName = "WLED Connection"

    @Transient
    override val pluginId = "wled"

    override suspend fun turnOn() = plugin?.turnOn()
        ?: throw IllegalStateException("Acquire this class from WledPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff()
        ?: throw IllegalStateException("Acquire this class from WledPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn()
        ?: throw IllegalStateException("Acquire this class from WledPowerPlugin!")

}