package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName

sealed class TradfriCommand(val command: String, @SerializedName("dev") val device: TradfriPowerDevice) {
    class TurnDeviceOn(device: TradfriPowerDevice) : TradfriCommand("turnOn", device)
    class TurnDeviceOff(device: TradfriPowerDevice) : TradfriCommand("turnOff", device)
    class GetDeviceStatus(device: TradfriPowerDevice) : TradfriCommand("checkStatus", device)
}

