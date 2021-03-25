package de.crysxd.octoapp.octoprint.models.printer

sealed class BedCommand(val command: String) {

    data class SetTargetTemperature(val target: Int) : BedCommand("target")

    data class SetTemperatureOffset(val offset: Int) : BedCommand("offset")

}

