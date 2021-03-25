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
    private val setTargetTemperaturesUseCase: SetTargetTemperaturesUseCase,
) : UseCase<CancelPrintJobUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PrintCancelledByApp)

        // Collect temps
        val current = if (param.restoreTemperatures) {
            timber.i("Capturing active temperature")
            octoPrintProvider.passiveCurrentMessageFlow("cancel_print_use_case_1").filter { it.temps.isNotEmpty() }.first()
        } else {
            null
        }

        // Issue cancel
        octoPrintProvider.octoPrint().createJobApi().executeJobCommand(JobCommand.CancelJobCommand)

        val temps = current?.temps?.firstOrNull()
        if (param.restoreTemperatures) {
            // Wait for print to be cancelled
            timber.i("Waiting for cancellation")
            octoPrintProvider.passiveCurrentMessageFlow("cancel_print_use_case_2").filter {
                it.state?.flags?.printing == false
            }.first()

            // Restore temps
            val targets = SetTargetTemperaturesUseCase.Params(
                listOf("tool0", "tool0", "tool0", "tool0", "bed", "chamber").map {
                    SetTargetTemperaturesUseCase.Temperature(component = it, temperature = temps?.components?.get(it)?.target?.toInt())
                }
            )
            setTargetTemperaturesUseCase.execute(targets)
        }
    }

    data class Params(
        val restoreTemperatures: Boolean
    )
}