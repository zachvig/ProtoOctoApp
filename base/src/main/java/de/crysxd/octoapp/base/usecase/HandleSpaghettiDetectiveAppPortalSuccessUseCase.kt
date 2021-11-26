package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject

class HandleSpaghettiDetectiveAppPortalSuccessUseCase @Inject constructor(val octoPrintRepository: OctoPrintRepository) : UseCase<Uri, Unit>() {

    override suspend fun doExecute(param: Uri, timber: Timber.Tree) {
        try {
            Timber.i("Handling connection result for Spaghetti Detective")
            val tunnelUrl = param.getQueryParameter("tunnel_endpoint")?.toHttpUrlOrNull()
            val isSuccess = tunnelUrl != null
            if (!isSuccess) {
                throw IllegalStateException("Connection was not successful")
            }

            octoPrintRepository.updateActive {
                it.copy(
                    alternativeWebUrl = tunnelUrl,
                    octoEverywhereConnection = null
                )
            }
        } catch (e: Exception) {
            OctoAnalytics.logEvent(OctoAnalytics.Event.SpaghettiDetectiveConnectFailed)
            throw e
        }

        OctoAnalytics.logEvent(OctoAnalytics.Event.SpaghettiDetectiveConnected)
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.SpaghettiDetectiveUser, "true")
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.RemoteAccess, "spaghetti_detective")
        Timber.i("Stored connection info for Spaghetti Detective")
    }
}