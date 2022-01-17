package de.crysxd.octoapp.octoprint.plugins.power.wemoswitch

import com.google.gson.annotations.SerializedName

sealed class WemoSwitchCommand(val command: String, @SerializedName("ip") val ipAddress: String) {
    class TurnDeviceOn(device: WemoSwitchPowerDevice) : WemoSwitchCommand("turnOn", device.id)
    class TurnDeviceOff(device: WemoSwitchPowerDevice) : WemoSwitchCommand("turnOff", device.id)
    class GetDeviceStatus(device: WemoSwitchPowerDevice) : WemoSwitchCommand("checkStatus", device.id)
}

