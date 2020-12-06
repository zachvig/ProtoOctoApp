package de.crysxd.octoapp.signin.usecases

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.SslKeyStoreHandler
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.login.LoginResponse
import de.crysxd.octoapp.signin.models.SignInInformation
import timber.log.Timber


class SignInUseCase(
    private val octoprintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
    private val sslKeyStoreHandler: SslKeyStoreHandler
) : UseCase<SignInInformation, SignInUseCase.Result>() {

    override suspend fun doExecute(param: SignInInformation, timber: Timber.Tree) = try {
        // Trust certificated
        param.trustedCerts?.let {
            timber.i("Trusting ${it.size} custom certificates")
            sslKeyStoreHandler.storeCertificates(it)
        }

        // Create OctoPrint
        val octoprintInstanceInformation = OctoPrintInstanceInformationV2(param.webUrl, param.apiKey)
        val octoprint = octoPrintProvider.createAdHocOctoPrint(octoprintInstanceInformation)

        // Test connection, will throw in case of faulty configuration
        val response = octoprint.createLoginApi().passiveLogin()
        val isAdmin = response.groups?.contains(LoginResponse.GROUP_ADMINS) == true

        // Test that the API key is actually valid. On instances without authentication
        // the login endpoint accepts any API key but other endpoints do not
        // Test with connection
        octoprint.createConnectionApi().getConnection()

        // Get version info
        val version = octoprint.createVersionApi().getVersion()
        Timber.i("Connected to ${version.serverVersionText}")
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.OctoPrintVersion, version.severVersion)
        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.UserIsAdmin, isAdmin.toString())

        // Check for warnings
        val testedVersion = OctoPrint.TESTED_SERVER_VERSION
        val warnings = mutableListOf<Warning>()
        if (!isAdmin) warnings.add(Warning.NotAdmin)
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
        object NotAdmin : Warning()
    }
}