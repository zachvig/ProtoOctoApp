package de.crysxd.octoapp.base.usecase

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject

class ExecuteGcodeCommandUseCase @Inject constructor() : UseCase<Pair<OctoPrint, GcodeCommand>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, GcodeCommand>) {
        Timber.i("Executing: ${param.second}")

        when (param.second) {
            is GcodeCommand.Single -> logEvent((param.second as GcodeCommand.Single).command)
            is GcodeCommand.Batch -> (param.second as GcodeCommand.Batch).commands.forEach { logEvent(it) }
        }

        param.first.createPrinterApi().executeGcodeCommand(param.second)
    }

    private fun logEvent(command: String) {
        Firebase.analytics.logEvent("gcode_send") {
            param("command", command)
        }
    }
}