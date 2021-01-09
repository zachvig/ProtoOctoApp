package de.crysxd.octoapp.base.usecase

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.isHlsStreamUrl
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
            val m115 = async {
                try {
                    executeM115()
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }

            val updated = current?.copy(
                m115Response = m115.await(),
                settings = settings.await()
            )

            val standardPlugins = Firebase.remoteConfig.getString("default_plugins").split(",").map { it.trim() }
            settings.await().plugins.keys.filter { !standardPlugins.contains(it) }.forEach {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PluginDetected(it))
            }

            OctoAnalytics.setUserProperty(
                OctoAnalytics.UserProperty.WebCamAvailable,
                isWebcamSupported(settings.await()) ?: "false"
            )

            if (updated != current) {
                timber.i("Updated capabilities: $updated")
                octoPrintRepository.storeOctoprintInstanceInformation(updated)
            } else {
                timber.i("No changes")
            }
        }
    }

    private fun isWebcamSupported(settings: Settings) = when {
        settings.webcam.streamUrl?.isHlsStreamUrl == true -> "hls"
        settings.webcam.streamUrl != null -> "mjpeg"
        else -> null
    }.takeIf { settings.webcam.webcamEnabled }

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