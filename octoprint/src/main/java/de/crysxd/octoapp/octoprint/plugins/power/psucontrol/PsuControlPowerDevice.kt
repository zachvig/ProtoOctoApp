package de.crysxd.octoapp.octoprint.plugins.power.psucontrol

import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class PsuControlPowerDevice(private val plugin: PsuControlPowerPlugin) : PowerDevice() {
    override val id = "psu"
    override val displayName = "PSU"
    override val pluginDisplayName = "PSU Control"

    override suspend fun turnOn() = plugin.turnOn()
    override suspend fun turnOff() = plugin.turnOff()
    override suspend fun isOn(): Boolean = plugin.isOn()
}
