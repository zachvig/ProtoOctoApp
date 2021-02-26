package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.resolve
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import timber.log.Timber
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<OctoPrintInstanceInformationV2?, List<WebcamSettings>>() {

    override suspend fun doExecute(param: OctoPrintInstanceInformationV2?, timber: Timber.Tree): List<WebcamSettings> {
        val instanceInfo = param ?: octoPrintRepository.getActiveInstanceSnapshot() ?: throw IllegalStateException("No OctoPrint available")
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(instanceInfo)
        val raw = instanceInfo.settings ?: octoPrint.createSettingsApi().getSettings()
        val webcamSettings = mutableListOf<WebcamSettings>()

        // Add all webcams from multicam plugin
        (raw.plugins.values.mapNotNull { it as? Settings.MultiCamSettings }.firstOrNull())?.profiles?.let { webcamSettings.addAll(it) }

        // If no webcams were added, use default settings object
        if (webcamSettings.isEmpty()) {
            webcamSettings.add(raw.webcam)
        }

        return webcamSettings.map {
            it.copy(
                multiCamUrl = null,
                standardStreamUrl = if (it.streamUrl?.startsWith("http") == false) {
                    val url = Uri.parse(octoPrint.webUrl)
                        .buildUpon()
                        .resolve(it.streamUrl)
                        .build()
                        .toString()
                    timber.i("Upgrading streamUrl from ${it.streamUrl} -> $url")
                    url
                } else {
                    it.streamUrl
                }
            )
        }
    }
}