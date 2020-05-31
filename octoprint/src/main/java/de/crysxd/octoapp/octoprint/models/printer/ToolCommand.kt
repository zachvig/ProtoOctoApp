package de.crysxd.octoapp.octoprint.models.printer

sealed class ToolCommand(val command: String) {

    data class SetTargetTemperatureToolCommand(val targets: TemperatureSet) : ToolCommand("target")

    data class SetTemperatureOffsetToolCommand(val targets: TemperatureSet) : ToolCommand("offset")

    data class TemperatureSet(val tool0: Int)

}

