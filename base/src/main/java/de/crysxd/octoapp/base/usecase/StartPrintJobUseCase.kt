package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileObject
import timber.log.Timber
import javax.inject.Inject

class StartPrintJobUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
) : UseCase<StartPrintJobUseCase.Params, StartPrintJobUseCase.Result>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): Result {
        val octoprint = octoPrintProvider.octoPrint()
        val settings = octoPrintRepository.getActiveInstanceSnapshot()?.settings ?: octoprint.createSettingsApi().getSettings()
        val materialManagerAvailable = octoprint.createMaterialManagerPluginsCollection().isMaterialManagerAvailable(settings)

        if (materialManagerAvailable && !param.materialSelectionConfirmed) {
            return Result.MaterialSelectionRequired
        }

        OctoAnalytics.logEvent(OctoAnalytics.Event.PrintStartedByApp)
        octoPrintProvider.octoPrint().createFilesApi().executeFileCommand(param.file, FileCommand.SelectFile(true))
        return Result.PrintStarted
    }

    data class Params(
        val file: FileObject.File,
        val materialSelectionConfirmed: Boolean
    )

    sealed class Result {
        object PrintStarted : Result()
        object MaterialSelectionRequired : Result()
    }
}