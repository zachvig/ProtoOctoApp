package de.crysxd.octoapp.octoprint.plugins.power.psucontrol

sealed class PsuCommand(val command: String) {
    object TurnOnPsuCommand : PsuCommand("turnPSUOn")
    object TurnOffPsuCommand : PsuCommand("turnPSUOff")
    object GetPsuStateCommand : PsuCommand("getPSUState")
}

