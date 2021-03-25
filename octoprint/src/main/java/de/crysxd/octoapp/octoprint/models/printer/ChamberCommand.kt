package de.crysxd.octoapp.octoprint.models.printer

sealed class ChamberCommand(val command: String) {

    data class SetTargetTemperature(val target: Int) : ChamberCommand("target")

    data class SetTemperatureOffset(val offset: Int) : ChamberCommand("offset")

}

