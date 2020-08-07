package de.crysxd.octoapp.base.usecase

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject

class ExecuteGcodeCommandUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val gcodeHistoryRepository: GcodeHistoryRepository
) : UseCase<ExecuteGcodeCommandUseCase.Param, Unit> {

    override suspend fun execute(param: Param) {
        Timber.i("Executing: ${param.command}")

        when (param.command) {
            is GcodeCommand.Single -> logExecuted(param.command.command, param.fromUser)
            is GcodeCommand.Batch -> param.command.commands.forEach { logExecuted(it, param.fromUser) }
        }

        val octoPrint = octoPrintProvider.octoPrint.value
        requireNotNull(octoPrint)
        octoPrint.createPrinterApi().executeGcodeCommand(param.command)
    }

    private fun logExecuted(command: String, fromUser: Boolean) {
        if (fromUser) {
            Firebase.analytics.logEvent("gcode_send") {
                param("command", command)
            }
            gcodeHistoryRepository.recordGcodeSend(command)
        }
    }

    data class Param(
        val command: GcodeCommand,
        val fromUser: Boolean
    )
}