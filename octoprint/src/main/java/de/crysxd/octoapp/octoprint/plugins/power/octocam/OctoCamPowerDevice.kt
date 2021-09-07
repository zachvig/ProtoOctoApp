package de.crysxd.octoapp.octoprint.plugins.power.octocam

import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class OctoCamPowerDevice(private val plugin: OctoCamPowerPlugin) : PowerDevice() {
    override val id = "octocam-torch"
    override val pluginId = "octocam"
    override val displayName = "OctoCam Torch"
    override val pluginDisplayName = "OctoCam"
    override val capabilities
        get() = listOf(Capability.Illuminate)

    override suspend fun turnOn() = plugin.turnOn()
    override suspend fun turnOff() = plugin.turnOff()
    override suspend fun toggle() = plugin.toggle()
    override suspend fun isOn(): Boolean = plugin.isOn()
}
