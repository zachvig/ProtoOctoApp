package de.crysxd.octoapp.octoprint.plugins.power.tradfri

sealed class TradfriCommand(val command: String, open val device: TradfriPowerDevice) {
    data class TurnDeviceOn(override val device: TradfriPowerDevice) : TradfriCommand("turnOn", device)
    data class TurnDeviceOff(override val device: TradfriPowerDevice) : TradfriCommand("turnOff", device)
    data class GetDeviceStatus(override val device: TradfriPowerDevice) : TradfriCommand("checkStatus", device)
}

