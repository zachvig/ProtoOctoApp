package de.crysxd.octoapp.octoprint.models.job

sealed class JobCommand(val command: String) {

    @Suppress("MayBeConstant", "unused")
    object PauseJobCommand : JobCommand("pause") {
        val action = "pause"
    }

    @Suppress("MayBeConstant", "unused")
    object ResumeJobCommand : JobCommand("pause") {
        val action = "resume"
    }

    @Suppress("MayBeConstant", "unused")
    object TogglePauseCommand : JobCommand("pause") {
        val action = "toggle"
    }

    object CancelJobCommand : JobCommand("cancel")

    object StartJobCommand : JobCommand("start")

    object RestartJobCommand : JobCommand("restart")

}