package de.crysxd.octoapp.octoprint.models.printer

sealed class PrintHeadCommand(val command: String) {

    data class JogPrintHeadCommand(val xDistance: Float = 0f, val yDistance: Float = 0f, val zDistance: Float = 0f) : PrintHeadCommand("jog")

    abstract class HomePrintHeadCommand(val axes: Array<String>) : PrintHeadCommand("home")

    object HomeXYAxisPrintHeadCommand : HomePrintHeadCommand(arrayOf("x", "y"))

    object HomeZAxisPrintHeadCommand : HomePrintHeadCommand(arrayOf("z"))

    object HomeAllAxisPrintHeadCommand : HomePrintHeadCommand(arrayOf("x", "y", "z"))

}

