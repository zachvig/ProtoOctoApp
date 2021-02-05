package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

class CancelPrintJobUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val setToolTemperatureUseCase: SetToolTargetTemperatureUseCase,
    private val setBedTemperatureUseCase: SetBedTargetTemperatureUseCase
) : UseCase<CancelPrintJobUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PrintCancelledByApp)

        // Collect temps
        val current = if (param.restoreTemperatures) {
            timber.i("Capturing active temperature")
            octoPrintProvider.passiveCurrentMessageFlow().filter { it.temps.isNotEmpty() }.first()
        } else {
            null
        }

        // Issue cancel
        octoPrintProvider.octoPrint().createJobApi().executeJobCommand(JobCommand.CancelJobCommand)

        val temps = current?.temps?.firstOrNull()
        if (param.restoreTemperatures) {
            // Wait for print to be cancelled
            timber.i("Waiting for cancellation")
            octoPrintProvider.passiveCurrentMessageFlow().filter {
                it.state?.flags?.printing == false
            }.first()

            // Restore temps
            timber.i("Restoring temperatures")
            temps?.tool0?.target?.toInt()?.let {
                timber.i("Restoring hotend to $it°C")
                setToolTemperatureUseCase.execute(SetToolTargetTemperatureUseCase.Param(toolTemperature = it))
            } ?: timber.w("Unable to restore hotend")

            temps?.bed?.target?.toInt()?.let {
                timber.i("Restoring bed to $it°C")
                setBedTemperatureUseCase.execute(SetBedTargetTemperatureUseCase.Param(bedTemperature = it))
            } ?: timber.w("Unable to restore bed")
        }
    }

    data class Params(
        val restoreTemperatures: Boolean
    )
}