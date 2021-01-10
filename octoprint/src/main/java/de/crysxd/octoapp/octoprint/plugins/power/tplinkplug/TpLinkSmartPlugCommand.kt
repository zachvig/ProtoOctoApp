package de.crysxd.octoapp.octoprint.plugins.power.tplinkplug

import com.google.gson.annotations.SerializedName

sealed class TpLinkSmartPlugCommand(val command: String, @SerializedName("ip") val ipAddress: String) {
    class TurnDeviceOn(device: TpLinkSmartPlugPowerDevice) : TpLinkSmartPlugCommand("turnOn", device.id)
    class TurnDeviceOff(device: TpLinkSmartPlugPowerDevice) : TpLinkSmartPlugCommand("turnOff", device.id)
    class GetDeviceStatus(device: TpLinkSmartPlugPowerDevice) : TpLinkSmartPlugCommand("checkStatus", device.id)
}

