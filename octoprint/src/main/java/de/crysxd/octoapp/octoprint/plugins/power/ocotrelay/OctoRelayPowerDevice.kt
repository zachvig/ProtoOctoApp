package de.crysxd.octoapp.octoprint.plugins.power.ocotrelay

import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class OctoRelayPowerDevice(
    private val plugin: OctoRelayPowerPlugin?,
    override val id: String,
    override val displayName: String,
) : PowerDevice() {
    override val pluginId = "octorelay"
    override val pluginDisplayName = "OctoRelay"
    override val capabilities get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)
    override val controlMethods get() = listOf(ControlMethod.Toggle, ControlMethod.TurnOnOff)

    override suspend fun turnOn() = plugin?.turnOn(id) ?: Unit
    override suspend fun turnOff() = plugin?.turnOff(id) ?: Unit
    override suspend fun toggle() = plugin?.toggle(id) ?: throw IllegalStateException("Get this device from the plugin to control it")
    override suspend fun isOn(): Boolean? = plugin?.getState(id)
}
