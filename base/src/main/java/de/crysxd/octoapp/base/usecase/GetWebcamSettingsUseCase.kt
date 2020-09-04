package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.resolve
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import timber.log.Timber
import javax.inject.Inject

class GetWebcamSettingsUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, WebcamSettings>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): WebcamSettings {
        val octoPrint = octoPrintProvider.octoPrint()
        val raw = octoPrint.createSettingsApi().getSettings().webcam
        return raw.copy(
            streamUrl = if (raw.streamUrl?.startsWith("http") == false) {
                val url = Uri.parse(octoPrint.webUrl)
                    .buildUpon()
                    .resolve(raw.streamUrl)
                    .build()
                    .toString()
                timber.i("Upgrading streamUrl from ${raw.streamUrl} -> $url")
                url
            } else {
                raw.streamUrl
            }
        )
    }
}