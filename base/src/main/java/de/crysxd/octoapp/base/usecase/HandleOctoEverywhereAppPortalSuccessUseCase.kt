package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.data.models.OctoEverywhereConnection
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import javax.inject.Inject

class HandleOctoEverywhereAppPortalSuccessUseCase @Inject constructor(val octoPrintRepository: OctoPrintRepository) : UseCase<Uri, Unit>() {

    override suspend fun doExecute(param: Uri, timber: Timber.Tree) {
        try {
            Timber.i("Handling connection result for OctoEverywhere")
            val isSuccess = param.getQueryParameter("success")?.toBoolean() == true
            if (!isSuccess) {
                throw IllegalStateException("Connection was not successful")
            }
            val connectionId = param.getQueryParameter("id") ?: throw IllegalStateException("No connection id given")
            val url = param.getQueryParameter("url")?.toHttpUrl() ?: throw IllegalStateException("No url given")
            val authUser = param.getQueryParameter("authbasichttpuser") ?: throw IllegalStateException("No auth user given")
            val authPw = param.getQueryParameter("authbasichttppassword") ?: throw IllegalStateException("No auth pw given")
            val authToken = param.getQueryParameter("authBearerToken") ?: throw IllegalStateException("No auth token given")
            val apiToken = param.getQueryParameter("appApiToken") ?: throw IllegalStateException("No api token given")
            val fullUrl = url.newBuilder()
                .password(authPw)
                .username(authUser)
                .build()

            octoPrintRepository.updateActive {
                it.copy(
                    alternativeWebUrl = fullUrl,
                    octoEverywhereConnection = OctoEverywhereConnection(
                        connectionId = connectionId,
                        apiToken = apiToken,
                        basicAuthPassword = authPw,
                        basicAuthUser = authUser,
                        bearerToken = authToken,
                        url = url,
                        fullUrl = fullUrl,
                    )
                )
            }
        } catch (e: Exception) {
            OctoAnalytics.logEvent(OctoAnalytics.Event.OctoEverywhereConnectFailed)
            throw e
        }

        OctoAnalytics.logEvent(OctoAnalytics.Event.OctoEverywhereConnected)
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.OctoEverywhereUser, "true")
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.RemoteAccess, "octoeverywhere")
        Timber.i("Stored connection info for OctoEverywhere")
    }
}