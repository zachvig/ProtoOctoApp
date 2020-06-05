package de.crysxd.octoapp.octoprint.models.printer

sealed class GcodeCommand {

    data class Single(val command: String) : GcodeCommand()
    data class Batch(val commands: Array<String>) : GcodeCommand()

}