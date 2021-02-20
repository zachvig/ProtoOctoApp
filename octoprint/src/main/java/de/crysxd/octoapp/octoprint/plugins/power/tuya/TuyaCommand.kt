package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName

sealed class TuyaCommand(val command: String, @SerializedName("label") val device: String) {
    class TurnDeviceOn(device: TuyaPowerDevice) : TuyaCommand("turnOn", device.displayName)
    class TurnDeviceOff(device: TuyaPowerDevice) : TuyaCommand("turnOff", device.displayName)
    class GetDeviceStatus(device: TuyaPowerDevice) : TuyaCommand("checkStatus", device.displayName)
}

