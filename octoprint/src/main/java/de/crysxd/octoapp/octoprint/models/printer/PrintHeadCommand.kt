package de.crysxd.octoapp.octoprint.models.printer

sealed class PrintHeadCommand(val command: String) {

    data class JogPrintHeadCommand(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val speed: Int = 500) : PrintHeadCommand("jog")

    abstract class HomePrintHeadCommand(val axes: Array<String>) : PrintHeadCommand("home")

    object HomeXYAxisPrintHeadCommand : HomePrintHeadCommand(arrayOf("x", "y"))

    object HomeZAxisPrintHeadCommand : HomePrintHeadCommand(arrayOf("z"))

    object HomeAllAxisPrintHeadCommand : HomePrintHeadCommand(arrayOf("x", "y", "z"))

}

