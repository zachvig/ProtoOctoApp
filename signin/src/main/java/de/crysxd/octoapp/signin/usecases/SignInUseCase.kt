package de.crysxd.octoapp.signin.usecases

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.SslKeyStoreHandler
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.models.login.LoginResponse
import de.crysxd.octoapp.signin.models.SignInInformation
import timber.log.Timber
import javax.inject.Inject


class SignInUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val sslKeyStoreHandler: SslKeyStoreHandler,
) : UseCase<SignInInformation, SignInUseCase.Result>() {

    override suspend fun doExecute(param: SignInInformation, timber: Timber.Tree) = try {
        // Trust certificated
        param.trustedCerts?.let {
            timber.i("Trusting ${it.size} custom certificates")
            sslKeyStoreHandler.storeCertificates(it)
        }
        if (param.weakHostNameVerificationRequired) {
            sslKeyStoreHandler.enforceWeakVerificationForHost(param.webUrl)
        }

        // Create OctoPrint
        val octoprintInstanceInformation = OctoPrintInstanceInformationV2(webUrl = param.webUrl, apiKey = param.apiKey)
        val octoprint = octoPrintProvider.createAdHocOctoPrint(octoprintInstanceInformation)

        // Test connection, will throw in case of faulty configuration
        val response = try {
            octoprint.createLoginApi().passiveLogin()
        } catch (e: KotlinNullPointerException) {
            // We received a 204. Retrofit is weird.
            Timber.w(e)
            throw InvalidApiKeyException()
        }

        if (response.session == null) {
            throw InvalidApiKeyException()
        }

        // Check admin status
        val isAdmin = response.groups?.contains(LoginResponse.GROUP_ADMINS) == true
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.UserIsAdmin, isAdmin.toString())

        // Test that the API key is actually valid. On instances without authentication
        // the login endpoint accepts any API key but other endpoints do not
        // Test with connection
        octoprint.createConnectionApi().getConnection()

        // Get version info
        val version = octoprint.createVersionApi().getVersion()
        Timber.i("Connected to ${version.serverVersionText}")
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.OctoPrintVersion, version.severVersion)

        // Check for warnings
        val testedVersion = Firebase.remoteConfig.getString("tested_octoprint_version")
        val warnings = mutableListOf<Warning>()
        if (version.severVersion > testedVersion) warnings.add(Warning.TooNewServerVersion(testedVersion, version.severVersion))

        Result.Success(octoprintInstanceInformation, warnings)
    } catch (e: Exception) {
        Timber.e(e)
        Result.Failure(e)
    }

    sealed class Result {
        data class Success(
            val octoPrintInstanceInformation: OctoPrintInstanceInformationV2,
            val warnings: List<Warning>
        ) : Result()

        data class Failure(
            val exception: java.lang.Exception
        ) : Result()
    }

    sealed class Warning {
        data class TooNewServerVersion(val testedOnVersion: String, val serverVersion: String) : Warning()
    }
}