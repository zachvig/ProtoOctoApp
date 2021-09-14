package de.crysxd.octoapp.octoprint.plugins.power.octocam

sealed class OctoCamCommand(val command: String) {
    object TurnLightOn : OctoCamCommand("turnOn")
    object TurnLightOff : OctoCamCommand("turnOff")
    object ToggleLight : OctoCamCommand("toggle")
    object CheckStatus : OctoCamCommand("checkStatus")
}

