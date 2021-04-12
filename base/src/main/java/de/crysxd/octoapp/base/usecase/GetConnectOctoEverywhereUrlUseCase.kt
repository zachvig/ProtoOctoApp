package de.crysxd.octoapp.base.usecase

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.octoprint.models.settings.Settings
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalStateException
import javax.inject.Inject


const val OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH = "connect-octoeverywhere"

class GetConnectOctoEverywhereUrlUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, GetConnectOctoEverywhereUrlUseCase.Result>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) = try {
        val printerId = try {
            octoPrintProvider.octoPrint().createOctoEverywhereApi().getInfo().printerId
        } catch (e: Exception) {
            Timber.w(e)
            null
        } ?: try {
            octoPrintProvider.octoPrint().createSettingsApi()
                .getSettings()
                .plugins.values
                .mapNotNull { it as? Settings.OctoEverywhere }
                .firstOrNull()
                ?.printerKey
        } catch (e: Exception) {
            Timber.w(e)
            null
        } ?: throw IllegalStateException("OctoEverywhere not installed")

        OctoAnalytics.logEvent(OctoAnalytics.Event.OctoEverywhereConnectStarted)
        Result.Success(
            Firebase.remoteConfig.getString("octoeverywhere_app_portal_url")
                .replace("{{{printerid}}}", printerId)
                .replace("{{{callbackPath}}}", OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH)
        )
    } catch (e: Exception) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.OctoEverywherePluginMissing)
        Result.Error(Injector.get().localizedContext().getString(R.string.configure_remote_acces___octoeverywhere___error_install_plugin), e)
    }


    sealed class Result {
        data class Error(val errorMessage: String, val exception: Exception) : Result()
        data class Success(val url: String) : Result()
    }
}