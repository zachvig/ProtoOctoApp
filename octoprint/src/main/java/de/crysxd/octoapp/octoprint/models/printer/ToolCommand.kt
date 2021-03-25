package de.crysxd.octoapp.octoprint.models.printer

sealed class ToolCommand(val command: String) {

    data class SetTargetTemperature(val targets: TemperatureSet) : ToolCommand("target")

    data class SetTemperatureOffset(val offsets: TemperatureSet) : ToolCommand("offset")

    data class ExtrudeFilament(val amount: Int) : ToolCommand("extrude")

    data class TemperatureSet(val tool0: Int?, val tool1: Int?, val tool2: Int?, val tool3: Int?)

}

