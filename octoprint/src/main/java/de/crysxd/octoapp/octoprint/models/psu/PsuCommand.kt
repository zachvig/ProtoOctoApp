package de.crysxd.octoapp.octoprint.models.psu

sealed class PsuCommand(val command: String) {
    object TurnOnPsuCommand : PsuCommand("turnPSUOn")
    object TurnOffPsuCommand : PsuCommand("turnPSUOff")
}

