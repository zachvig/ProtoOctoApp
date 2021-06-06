package de.crysxd.octoapp.octoprint.plugins.power.mystrom

import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlPowerPlugin

data class MyStromPowerDevice(private val plugin: MyStromPowerPlugin) : PowerDevice() {
    override val id = "relay"
    override val pluginId = "mystromswitch"
    override val displayName = "myStrom"
    override val pluginDisplayName = "myStrom"
    override val capabilities get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)

    override suspend fun turnOn() = plugin.turnOn()
    override suspend fun turnOff() = plugin.turnOff()
    override suspend fun isOn(): Boolean? = null
}
