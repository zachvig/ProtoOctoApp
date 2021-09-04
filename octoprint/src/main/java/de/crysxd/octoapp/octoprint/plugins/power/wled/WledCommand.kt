package de.crysxd.octoapp.octoprint.plugins.power.wled

sealed class WledCommand(val command: String) {
    object TurnDeviceOn : WledCommand("lights_on")
    object TurnDeviceOff : WledCommand("lights_off")
}

