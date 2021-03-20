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
) : UseCase<UpdateInstanceCapabilitiesUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        withContext(Dispatchers.IO) {
            // Perform online check. This will trigger switching to the primary web url
            // if we currently use a cloud/backup connection
            if (octoPrintRepository.getActiveInstanceSnapshot()?.alternativeWebUrl != null) {
                timber.i("Checking for primary web url being online")
                octoPrintProvider.octoPrint().performOnlineCheck()
            }

            octoPrintRepository.updateActive { current ->
                // Gather all info in parallel
                val settings = async { octoPrintProvider.octoPrint().createSettingsApi().getSettings() }
                val commands = async {
                    try {
                        octoPrintProvider.octoPrint().createSystemApi().getSystemCommands()
                    } catch (e: Exception) {
                        // Might fail for lacking permissions
                        Timber.e(e)
                        null
                    }
                }

                val m115 = async {
                    try {
                        if (param.updateM115) {
                            executeM115()
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                        null
                    }
                }

                val updated = current.copy(
                    m115Response = m115.await() ?: current.m115Response,
                    settings = settings.await(),
                    systemCommands = commands.await()?.all
                )

                val standardPlugins = Firebase.remoteConfig.getString("default_plugins").split(",").map { it.trim() }
                settings.await().plugins.keys.filter { !standardPlugins.contains(it) }.forEach {
                    OctoAnalytics.logEvent(OctoAnalytics.Event.PluginDetected(it))
                }

                OctoAnalytics.setUserProperty(
                    OctoAnalytics.UserProperty.WebCamAvailable,
                    isWebcamSupported(settings.await()) ?: "false"
                )

                timber.i("Updated capabilities: $updated")
                updated
            }
        }
    }

    private fun isWebcamSupported(settings: Settings) = when {
        settings.webcam.streamUrl?.isHlsStreamUrl == true -> "hls"
        settings.webcam.streamUrl != null -> "mjpeg"
        else -> null
    }.takeIf { settings.webcam.webcamEnabled != false }

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

    data class Params(
        val updateM115: Boolean = true
    )
}