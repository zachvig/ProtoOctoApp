package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import okhttp3.HttpUrl
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

class ExtrudeFilamentUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : UseCase<ExtrudeFilamentUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        val octoPrint = octoPrintProvider.octoPrint()

        // Check if printing
        // When we are printing, we don't check temperatures. Usually this means we are paused because the extrude controls
        // are only available during pause, but we don't care here. M302 is not reliable during prints/paused so we skip it
        // and let OctoPrint/the printer handle cold extrude (#948)
        val state = octoPrint.createPrinterApi().getPrinterState()
        val currentTemp = state.temperature?.tool0?.actual?.toInt() ?: Int.MAX_VALUE
        val isPrinting = state.state?.flags?.isPrinting() == true

        // Check if we can actually extrude. Some older Marlin printers will crash
        // if we attempt a cold extrusion
        val minTemp = try {
            if (isPrinting) {
                timber.i("Print active, omitting min temperature request and assuming very low minimum of 50°C")
                50
            } else {
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

                timber.i("Determined temperatures:  minTemp=$minTemp currentTemp=$currentTemp")
                minTemp
            }
        } catch (e: Exception) {
            // We tried our best, let's continue without temp check
            Timber.e(e)
            0
        }

        // Check if current temp is below minimum
        if (minTemp > currentTemp) {
            throw ColdExtrusionException(
                minTemp = minTemp,
                currentTemp = currentTemp,
                octoPrintProvider.octoPrint().fullWebUrl,
            )
        }

        // Extrude, temperature ok or unknown
        octoPrintProvider.octoPrint().createPrinterApi().executeToolCommand(
            ToolCommand.ExtrudeFilament(param.extrudeLengthMm)
        )
    }

    data class Param(
        val extrudeLengthMm: Int
    )

    class ColdExtrusionException(val minTemp: Int, val currentTemp: Int, webUrl: HttpUrl) :
        OctoPrintException(webUrl = webUrl, userFacingMessage = BaseInjector.get().localizedContext().getString(R.string.error_cold_extrusion))
}