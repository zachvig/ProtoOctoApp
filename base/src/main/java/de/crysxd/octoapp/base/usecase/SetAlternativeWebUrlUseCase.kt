package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.models.settings.Settings
import timber.log.Timber
import javax.inject.Inject

class SetAlternativeWebUrlUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
) : UseCase<SetAlternativeWebUrlUseCase.Params, SetAlternativeWebUrlUseCase.Result>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): Result {
        if (!param.bypassChecks && param.webUrl.isNotEmpty()) {
            val instance = octoPrintRepository.getActiveInstanceSnapshot()
                ?: throw IllegalStateException("No active instance")
            val uri = try {
                Uri.parse(param.webUrl)
            } catch (e: Exception) {
                Timber.e(e)
                OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySetFailed)
                return Result.Failure("Please provide a valid URL", e)
            }

            val settings = try {
                val octoprint = octoPrintProvider.createAdHocOctoPrint(instance.copy(webUrl = uri.toString(), alternativeWebUrl = null))
                octoprint.createSettingsApi().getSettings()
            } catch (e: Exception) {
                Timber.e(e)
                OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySetFailed)
                return Result.Failure("Unable to connect", e)
            }

            try {
                val remoteUuid = settings.plugins.values.mapNotNull { it as? Settings.Discovery }.firstOrNull()
                val localUuid = octoPrintProvider.octoPrint().createSettingsApi().getSettings()
                    .plugins.values.mapNotNull { it as? Settings.Discovery }.firstOrNull()

                if (localUuid != remoteUuid) {
                    throw IllegalStateException("Upnp UUIDs for primary and alternate URLs differ: $localUuid <--> $remoteUuid")
                }
            } catch (e: Exception) {
                OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySetFailed)
                return Result.Failure("Unable to verify that the remote URL points to the same instance", e, true)
            }
        }

        octoPrintRepository.updateActive {
            it.copy(alternativeWebUrl = param.webUrl.takeIf { s -> s.isNotBlank() }, octoEverywhereConnection = null)
        }

        OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySet)
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.RemoteAccess, "manual")
        return Result.Success
    }

    data class Params(
        val webUrl: String,
        val bypassChecks: Boolean
    )

    sealed class Result {
        object Success : Result()
        data class Failure(
            val errorMessage: String,
            val exception: Exception,
            val allowToProceed: Boolean = false
        ) : Result()
    }
}