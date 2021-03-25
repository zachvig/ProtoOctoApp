package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.BedCommand
import de.crysxd.octoapp.octoprint.models.printer.ChamberCommand
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import timber.log.Timber
import javax.inject.Inject

class SetTargetTemperaturesUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<SetTargetTemperaturesUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        param.temps.filter { it.temperature != null }.forEach {
            when {
                it.component.startsWith("tool") -> {
                    octoPrintProvider.octoPrint().createPrinterApi().executeToolCommand(
                        ToolCommand.SetTargetTemperature(
                            ToolCommand.TemperatureSet(
                                tool0 = it.temperature.takeIf { _ -> it.component == "tool0" },
                                tool1 = it.temperature.takeIf { _ -> it.component == "tool1" },
                                tool2 = it.temperature.takeIf { _ -> it.component == "tool2" },
                                tool3 = it.temperature.takeIf { _ -> it.component == "tool3" },
                            )
                        )
                    )
                }

                it.component == "bed" -> {
                    octoPrintProvider.octoPrint().createPrinterApi().executeBedCommand(
                        BedCommand.SetTargetTemperature(it.temperature ?: 0)
                    )
                }

                it.component == "chamber" -> {
                    octoPrintProvider.octoPrint().createPrinterApi().executeChamberCommand(
                        ChamberCommand.SetTargetTemperature(it.temperature ?: 0)
                    )
                }
            }
        }
    }

    data class Params(
        val temps: List<Temperature>
    ) {
        constructor(temp: Temperature) : this(listOf(temp))
    }

    data class Temperature(
        val temperature: Int?,
        val component: String
    )
}