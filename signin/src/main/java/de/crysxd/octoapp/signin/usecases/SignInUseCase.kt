package de.crysxd.octoapp.signin.usecases

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.login.LoginResponse
import de.crysxd.octoapp.signin.models.SignInInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber


class SignInUseCase(
    private val octoprintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<SignInInformation, SignInUseCase.Result> {

    override suspend fun execute(param: SignInInformation): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val octoprintInstanceInformation = OctoPrintInstanceInformationV2(
                param.webUrl,
                param.apiKey
            )

            val octoprint = octoPrintProvider.createAdHocOctoPrint(octoprintInstanceInformation)

            // Test connection, will throw in case of faulty configuration
            val response = octoprint.createLoginApi().passiveLogin()
            val isAdmin = response.groups.contains(LoginResponse.GROUP_ADMINS)

            // Get version info
            val version = octoprint.createVersionApi().getVersion()
            Timber.i("Connected to ${version.serverVersionText}")
            Firebase.analytics.setUserProperty("octoprint_api_version", version.apiVersion)
            Firebase.analytics.setUserProperty("octoprint_server_version", version.severVersion)
            Firebase.analytics.setUserProperty("octoprint_server_admin", isAdmin.toString())

            // Check for warnings
            val testedVersion = OctoPrint.TESTED_SERVER_VERSION
            val warnings = mutableListOf<Warning>()
            if (!isAdmin) warnings.add(Warning.NotAdmin)
            if (version.severVersion > testedVersion) warnings.add(Warning.TooNewServerVersion(testedVersion, version.severVersion))

            Result.Success(octoprintInstanceInformation, warnings)
        } catch (e: Exception) {
            Timber.e(e)
            Result.Failure
        }
    }

    sealed class Result {
        data class Success(
            val octoPrintInstanceInformation: OctoPrintInstanceInformationV2,
            val warnings: List<Warning>
        ) : Result()

        object Failure : Result()
    }

    sealed class Warning {
        data class TooNewServerVersion(val testedOnVersion: String, val serverVersion: String) : Warning()
        object NotAdmin : Warning()
    }
}