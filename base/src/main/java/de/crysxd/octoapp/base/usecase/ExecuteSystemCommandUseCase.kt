package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.system.SystemCommand
import timber.log.Timber
import javax.inject.Inject

class ExecuteSystemCommandUseCase @Inject constructor(private val octoPrintProvider: OctoPrintProvider) : UseCase<SystemCommand, Unit>() {
    override suspend fun doExecute(param: SystemCommand, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createSystemApi().executeSystemCommand(param)
    }
}