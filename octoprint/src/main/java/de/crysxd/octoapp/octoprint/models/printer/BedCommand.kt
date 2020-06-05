package de.crysxd.octoapp.octoprint.models.printer

sealed class BedCommand(val command: String) {

    data class SetTargetTemperatureToolCommand(val target: Int) : BedCommand("target")

    data class SetTemperatureOffsetToolCommand(val offset: Int) : BedCommand("offset")

}

