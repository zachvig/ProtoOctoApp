package de.crysxd.octoapp.base.usecase

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintApiException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintUnavailableException
import timber.log.Timber
import javax.inject.Inject


class GetRemoteServiceConnectUrlUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<GetRemoteServiceConnectUrlUseCase.RemoteService, GetRemoteServiceConnectUrlUseCase.Result>() {

    companion object {
        const val OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH = "connect-octoeverywhere"
        const val SPAGHETTI_DETECTIVE_APP_PORTAL_CALLBACK_PATH = "connect-spaghetti-detective"
    }

    override suspend fun doExecute(param: RemoteService, timber: Timber.Tree) = try {
        val printerId = param.getPrinterId(octoPrintProvider.octoPrint())
        val url = param.getConnectUrl()
            .replace("{{{printerid}}}", printerId ?: "")
            .replace("{{{callbackPath}}}", param.getCallbackPath())
        Result.Success(url)
    } catch (e: Exception) {
        val message = param.getMessageForException(e) ?: R.string.error_general
        Result.Error(BaseInjector.get().localizedContext().getString(message), e)
    }

    class OctoEverywhereNotInstalledException : IllegalStateException("OctoEverywhere not installed")

    sealed class RemoteService {
        abstract suspend fun getPrinterId(octoPrint: OctoPrint): String?
        abstract fun getConnectUrl(): String
        abstract fun getCallbackPath(): String
        abstract fun recordStartEvent()
        abstract fun getMessageForException(e: Exception): Int?

        object OctoEverywhere : RemoteService() {
            override fun getConnectUrl() = Firebase.remoteConfig.getString("octoeverywhere_app_portal_url")
            override fun getCallbackPath() = OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH
            override fun recordStartEvent() = OctoAnalytics.logEvent(OctoAnalytics.Event.OctoEverywhereConnectStarted)
            override suspend fun getPrinterId(octoPrint: OctoPrint) = try {
                octoPrint.createOctoEverywhereApi().getInfo().printerId
            } catch (e: OctoPrintApiException) {
                if (e.responseCode == 400 || e.responseCode == 404) {
                    OctoAnalytics.logEvent(OctoAnalytics.Event.OctoEverywherePluginMissing)
                    throw OctoEverywhereNotInstalledException()
                } else {
                    throw e
                }
            }

            override fun getMessageForException(e: java.lang.Exception) = when (e) {
                is OctoEverywhereNotInstalledException -> R.string.configure_remote_acces___octoeverywhere___error_install_plugin
                is OctoPrintUnavailableException -> R.string.configure_remote_acces___octoeverywhere___error_no_connection
                else -> null
            }
        }

        object SpaghettiDetective : RemoteService() {
            override suspend fun getPrinterId(octoPrint: OctoPrint) = try {
                octoPrint.createSpaghettiDetectiveApi().getLinkedPrinterId() ?: ""
            } catch (e: OctoPrintApiException) {
                Timber.e(e)
                null
            }

            override fun recordStartEvent() = OctoAnalytics.logEvent(OctoAnalytics.Event.SpaghettiDetectiveConnectStarted)
            override fun getCallbackPath() = SPAGHETTI_DETECTIVE_APP_PORTAL_CALLBACK_PATH
            override fun getConnectUrl() = Firebase.remoteConfig.getString("spaghetti_detective_app_portal_url")
            override fun getMessageForException(e: Exception): Int? = null
        }
    }

    sealed class Result {
        data class Error(val errorMessage: String, val exception: Exception) : Result()
        data class Success(val url: String) : Result()
    }
}