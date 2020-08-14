package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.ext.appendFullPath
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor() : UseCase<GetWebcamSettingsUseCase.Params, WebcamSettings> {

    override suspend fun execute(param: Params): WebcamSettings {
        val raw = param.octoPrint.createSettingsApi().getSettings().webcam
        return raw.copy(
            streamUrl = if (raw.streamUrl.startsWith("/")) {
                Uri.parse(param.octoPrint.webUrl)
                    .buildUpon()
                    .appendFullPath(raw.streamUrl)
                    .build()
                    .toString()
            } else {
                raw.streamUrl
            }
        )
    }

    data class Params(
        val octoPrint: OctoPrint
    )
}