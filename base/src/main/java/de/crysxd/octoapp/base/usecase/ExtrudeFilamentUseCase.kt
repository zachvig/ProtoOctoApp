package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.exceptions.UserMessageException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

class ExtrudeFilamentUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : UseCase<ExtrudeFilamentUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        val octoPrint = octoPrintProvider.octoPrint()

        // Check if we can actually extrude. Some older Marlin printers will crash
        // if we attempt a cold extrusion
        val (minTemp, currentTemp) = try {
            // Check minimum extrusion temp
            val response = executeGcodeCommandUseCase.execute(
                ExecuteGcodeCommandUseCase.Param(
                    command = GcodeCommand.Single("M302"),
                    fromUser = false,
                    recordResponse = true
                )
            )
            val m302ResponsePattern = Pattern.compile("^Recv:\\s+echo:.*(disabled|enabled).*min\\s+temp\\s+(\\d+)")
            val minTemp = response.mapNotNull {
                (it as? ExecuteGcodeCommandUseCase.Response.RecordedResponse)?.responseLines
            }.flatten().mapNotNull {
                val matcher = m302ResponsePattern.matcher(it)
                if (matcher.find()) {
                    val disabled = matcher.group(1) == "disabled"
                    val minTemp = matcher.group(2)?.toInt() ?: 0
                    if (disabled) minTemp else 0
                } else {
                    null
                }
            }.firstOrNull() ?: let {
                timber.e("Unable to get min temp from response: $response")
                0
            }

            // Check current temp
            val state = octoPrint.createPrinterApi().getPrinterState()
            val currentTemp = state.temperature?.tool0?.actual?.toInt()

            timber.i("Determined temperatures:  minTemp=$minTemp currentTemp=$currentTemp")
            Pair(minTemp, currentTemp)
        } catch (e: Exception) {
            // We tried our best, let's continue without temp check
            Timber.e(e)
            Pair(null, null)
        }

        // Check if current temp is below minimum
        if (minTemp != null && currentTemp != null && minTemp > currentTemp) {
            throw ColdExtrusionException(
                minTemp = minTemp,
                currentTemp = currentTemp
            )
        }

        // Extrude, temperature ok or unknown
        octoPrintProvider.octoPrint().createPrinterApi().executeToolCommand(
            ToolCommand.ExtrudeFilamentToolCommand(param.extrudeLengthMm)
        )
    }

    data class Param(
        val extrudeLengthMm: Int
    )

    class ColdExtrusionException(override val userMessage: Int = R.string.error_cold_extrusion, val minTemp: Int, val currentTemp: Int) : OctoPrintException(),
        UserMessageException
}