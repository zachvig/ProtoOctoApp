package de.crysxd.octoapp.base.usecase

import android.content.Context
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.modules.AndroidModule
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.isOctoEverywhereUrl
import de.crysxd.octoapp.octoprint.isSharedOctoEverywhereUrl
import de.crysxd.octoapp.octoprint.models.settings.Settings
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SetAlternativeWebUrlUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
    @Named(AndroidModule.LOCALIZED) private val context: Context
) : UseCase<SetAlternativeWebUrlUseCase.Params, SetAlternativeWebUrlUseCase.Result>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): Result {
        val instance = octoPrintRepository.getActiveInstanceSnapshot()
            ?: throw IllegalStateException("No active instance")
        val octoPrint = octoPrintProvider.octoPrint()

        val url = if (param.webUrl.isNotEmpty()) {
            try {
                param.webUrl.toHttpUrl().newBuilder()
                    .password(param.password)
                    .username(param.username)
                    .build()
            } catch (e: Exception) {
                Timber.e(e)
                OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySetFailed)
                return Result.Failure(context.getString(R.string.configure_remote_acces___manual___error_invalid_url), e)
            }
        } else {
            null
        }

        if (!param.bypassChecks && url != null) {
            val isOe = url.isOctoEverywhereUrl()
            val isShared = url.isSharedOctoEverywhereUrl()
            when {
                isOe && !isShared -> return Result.Failure(
                    errorMessage = context.getString(R.string.configure_remote_acces___manual___error_normal_octoeverywhere_url),
                    allowToProceed = false,
                    exception = IllegalArgumentException("Given URL is a standard OctoEverywhere URL")
                )
                isOe && isShared -> return Result.Failure(
                    errorMessage = context.getString(R.string.configure_remote_acces___manual___error_shared_octoeverywhere_url),
                    allowToProceed = true,
                    exception = IllegalArgumentException("Given URL is a shared OctoEverywhere URL")
                )
            }

            val settings = try {
                val octoprint = octoPrintProvider.createAdHocOctoPrint(instance.copy(webUrl = url))
                octoprint.createSettingsApi().getSettings()
            } catch (e: Exception) {
                Timber.e(e)
                OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySetFailed)
                return Result.Failure(context.getString(R.string.configure_remote_acces___manual___error_unable_to_connect), e)
            }

            try {
                val remoteUuid = settings.plugins.values.mapNotNull { it as? Settings.Discovery }.firstOrNull()?.uuid
                val localUuid = octoPrint.createSettingsApi().getSettings()
                    .plugins.values.mapNotNull { it as? Settings.Discovery }.firstOrNull()?.uuid

                if (localUuid != remoteUuid) {
                    throw IllegalStateException("Upnp UUIDs for primary and alternate URLs differ: $localUuid <--> $remoteUuid")
                }
            } catch (e: Exception) {
                OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySetFailed)
                return Result.Failure(context.getString(R.string.configure_remote_acces___manual___error_unable_to_verify), e, true)
            }
        }

        octoPrintRepository.update(instance.id) {
            it.copy(alternativeWebUrl = url, octoEverywhereConnection = null)
        }

        OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigManuallySet)
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.RemoteAccess, "manual")
        return Result.Success
    }

    data class Params(
        val webUrl: String,
        val username: String,
        val password: String,
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