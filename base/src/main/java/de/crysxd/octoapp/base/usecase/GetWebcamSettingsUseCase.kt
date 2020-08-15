package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import timber.log.Timber
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor() : UseCase<GetWebcamSettingsUseCase.Params, WebcamSettings>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): WebcamSettings {
        val raw = param.octoPrint.createSettingsApi().getSettings().webcam
        return raw.copy(
            streamUrl = if (raw.streamUrl.startsWith("/")) {
                val url = "${param.octoPrint.webUrl}${raw.streamUrl.replaceFirst("/", "")}"
                timber.i("Upgrading streamUrl from ${raw.streamUrl} -> $url")
                url
            } else {
                raw.streamUrl
            }
        )
    }

    data class Params(
        val octoPrint: OctoPrint
    )
}