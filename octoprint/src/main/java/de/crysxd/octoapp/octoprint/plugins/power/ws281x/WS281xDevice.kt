package de.crysxd.octoapp.octoprint.plugins.power.ws281x

import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

sealed class WS281xDevice : PowerDevice() {
    abstract override val id: String
    abstract val plugin: WS281xPowerPlugin?

    override val capabilities
        get() = listOf(Capability.Illuminate)

    @Transient
    override val pluginDisplayName = "WS281x LED Status"

    @Transient
    override val pluginId = "ws281x_led_status"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from TuyaPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from TuyaPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from TuyaPowerPlugin!")

    data class WS281xTorchDevice(
        @Transient override val plugin: WS281xPowerPlugin?,
    ) : WS281xDevice() {
        override val id: String = "$pluginId/torch"
        override val displayName: String = "WS281x Torch"
    }

    data class WS281xLightsDevice(
        @Transient override val plugin: WS281xPowerPlugin?,
    ) : WS281xDevice() {
        override val id: String = "$pluginId/lights"
        override val displayName = "WS281x Lights"
    }
}