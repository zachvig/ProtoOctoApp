package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil

class TunePrintUseCase @Inject constructor(
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : UseCase<TunePrintUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        val commands = mutableListOf<String>()
        param.feedRate?.coerceIn(0..250)?.let { commands.add("M220 S$it") }
        param.flowRate?.coerceIn(0..250)?.let { commands.add("M221 S$it") }
        param.fanSpeed?.coerceIn(0..100)?.let { commands.add("M106 S${ceil(it * 2.55).toInt()}") }

        executeGcodeCommandUseCase.execute(
            ExecuteGcodeCommandUseCase.Param(
                command = GcodeCommand.Batch(commands.toTypedArray()),
                fromUser = false
            )
        )
    }

    data class Param(
        val feedRate: Int?,
        val flowRate: Int?,
        val fanSpeed: Int?
    )
}