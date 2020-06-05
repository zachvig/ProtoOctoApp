package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber

class ExecuteGcodeCommandUseCase : UseCase<Pair<OctoPrint, GcodeCommand>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, GcodeCommand>) {
        Timber.i("Executing: ${param.second}")
        param.first.createPrinterApi().executeGcodeCommand(param.second)
    }
}