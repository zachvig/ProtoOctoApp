package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor() : UseCase<GetWebcamSettingsUseCase.Params, WebcamSettings> {

    override suspend fun execute(param: Params): WebcamSettings {
        val raw = param.octoPrint.createSettingsApi().getSettings().webcam
        return raw.copy(
            streamUrl = if (raw.streamUrl.startsWith("/")) {
                "${param.octoPrint.webUrl}${raw.streamUrl.replaceFirst("/", "")}"
            } else {
                raw.streamUrl
            }
        )
    }

    data class Params(
        val octoPrint: OctoPrint
    )
}