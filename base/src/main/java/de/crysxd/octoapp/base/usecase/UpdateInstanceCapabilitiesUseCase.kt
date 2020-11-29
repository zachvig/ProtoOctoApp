package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.models.settings.Settings
import timber.log.Timber
import javax.inject.Inject

class UpdateInstanceCapabilitiesUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        val current = octoPrintRepository.getRawOctoPrintInstanceInformation()

        val settings = octoPrintProvider.octoPrint().createSettingsApi().getSettings()
        val updated = current?.copy(
            supportsWebcam = isWebcamSupported(settings),
            supportsPsuPlugin = isPsuControlSupported(settings)
        )

        timber.i("Updated capabilities: $updated")

        octoPrintRepository.storeOctoprintInstanceInformation(updated)
    }

    private fun isWebcamSupported(settings: Settings) = settings.webcam.webcamEnabled

    private fun isPsuControlSupported(settings: Settings) = settings.plugins.settings.containsKey("psucontrol")

}