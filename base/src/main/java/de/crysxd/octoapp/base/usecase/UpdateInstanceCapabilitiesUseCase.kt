package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

class UpdateInstanceCapabilitiesUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        withContext(Dispatchers.IO) {
            val current = octoPrintRepository.getRawOctoPrintInstanceInformation()

            // Gather all info in parallel
            val settings = async { octoPrintProvider.octoPrint().createSettingsApi().getSettings() }
            val m115 = async { executeM115() }

            val updated = current?.copy(
                supportsWebcam = isWebcamSupported(settings.await()),
                supportsPsuPlugin = isPsuControlSupported(settings.await()),
                m115Response = m115.await()
            )

            timber.i("Updated capabilities: $updated")

            if (updated != current) {
                octoPrintRepository.storeOctoprintInstanceInformation(updated)
            } else {
                timber.i("No changes")
            }
        }
    }

    private fun isWebcamSupported(settings: Settings) = settings.webcam.webcamEnabled

    private fun isPsuControlSupported(settings: Settings) = settings.plugins.settings.containsKey("psucontrol")

    private suspend fun executeM115() = try {
        withTimeout(5000L) {
            executeGcodeCommandUseCase.execute(
                ExecuteGcodeCommandUseCase.Param(
                    GcodeCommand.Single("M115"),
                    recordResponse = true,
                    fromUser = false
                )
            )
        }.let {
            val response = it.firstOrNull() as? ExecuteGcodeCommandUseCase.Response.RecordedResponse
            response?.responseLines?.joinToString("\n")
        }
    } catch (e: Exception) {
        Timber.e(e)
        // We do not escalate this error. Fallback to empty.
        null
    }
}