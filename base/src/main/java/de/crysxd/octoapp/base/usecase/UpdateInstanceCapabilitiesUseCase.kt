package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class UpdateInstanceCapabilitiesUseCase @Inject constructor() : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint) {
        withContext(Dispatchers.Default) {
            Timber.i("Updating capabilities...")

            val repository = Injector.get().octorPrintRepository()
            val current = repository.getRawOctoPrintInstanceInformation()

            val settings = param.createSettingsApi().getSettings()
            val updated = current?.copy(
                supportsWebcam = isWebcamSupported(settings),
                supportsPsuPlugin = isPsuControlSupported(settings)
            )

            Timber.i("Updated capabilities: $updated")

            repository.storeOctoprintInstanceInformation(updated)
        }
    }

    private fun isWebcamSupported(settings: Settings) = settings.webcam.webcamEnabled

    private fun isPsuControlSupported(settings: Settings) = settings.plugins.containsKey("psucontrol")

}