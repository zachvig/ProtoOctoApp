package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.resolve
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import timber.log.Timber
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<OctoPrintInstanceInformationV2?, WebcamSettings>() {

    override suspend fun doExecute(param: OctoPrintInstanceInformationV2?, timber: Timber.Tree): WebcamSettings {
        val instanceInfo = param ?: octoPrintRepository.getActiveInstanceSnapshot() ?: throw IllegalStateException("No OctoPrint available")
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(instanceInfo)
        val raw = instanceInfo.settings ?: octoPrint.createSettingsApi().getSettings()

        val webcam = raw.webcam
        return webcam.copy(
            streamUrl = if (webcam.streamUrl?.startsWith("http") == false) {
                val url = Uri.parse(octoPrint.webUrl)
                    .buildUpon()
                    .resolve(webcam.streamUrl)
                    .build()
                    .toString()
                timber.i("Upgrading streamUrl from ${webcam.streamUrl} -> $url")
                url
            } else {
                webcam.streamUrl
            }
        )
    }
}