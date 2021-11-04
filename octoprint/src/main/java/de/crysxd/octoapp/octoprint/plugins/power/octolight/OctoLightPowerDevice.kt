package de.crysxd.octoapp.octoprint.plugins.power.octolight

import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class OctoLightPowerDevice(private val plugin: OctoLightPowerPlugin) : PowerDevice() {
    override val id = "octolight"
    override val pluginId = "octolight"
    override val displayName = "OctoLight"
    override val pluginDisplayName = "OctoLight"
    override val capabilities
        get() = listOf(Capability.Illuminate)
    override val controlMethods: List<ControlMethod>
        get() = listOf(ControlMethod.Toggle)

    override suspend fun turnOn() = throw UnsupportedOperationException()
    override suspend fun turnOff() = throw UnsupportedOperationException()
    override suspend fun toggle() = plugin.toggle()
    override suspend fun isOn(): Boolean? = null
}