package de.crysxd.octoapp.octoprint.models.printer

sealed class ToolCommand(val command: String) {

    data class SetTargetTemperatureToolCommand(val targets: TemperatureSet) : ToolCommand("target")

    data class SetTemperatureOffsetToolCommand(val offsets: TemperatureSet) : ToolCommand("offset")

    data class ExtrudeFilamentToolCommand(val amount: Int) : ToolCommand("extrude")

    data class TemperatureSet(val tool0: Int)

}

