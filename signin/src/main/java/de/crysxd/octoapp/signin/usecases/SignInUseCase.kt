package de.crysxd.octoapp.signin.usecases

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.signin.models.SignInInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber


class SignInUseCase(
    private val octoprintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<SignInInformation, Boolean> {

    override suspend fun execute(param: SignInInformation): Boolean = withContext(Dispatchers.IO) {
        val octoprintInstanceInformation = OctoPrintInstanceInformation(
            param.ipAddress,
            param.port.toInt(),
            param.apiKey
        )

        val octoprint = octoPrintProvider.createAdHocOctoPrint(octoprintInstanceInformation)

        try {
            // Test connection, will throw in case of faulty configuration
            octoprint.createConnectionApi().getConnection()

            // Get version info
            val version = octoprint.createVersionApi().getVersion()
            Timber.i("Connected to ${version.serverVersionText}")
            Firebase.analytics.setUserProperty("octoprint_api_version", version.apiVersion)
            Firebase.analytics.setUserProperty("octoprint_server_version", version.severVersion)
        } catch (e: Exception) {
            Timber.e(e)
            return@withContext false
        }

        octoprintRepository.storeOctoprintInstanceInformation(octoprintInstanceInformation)
        true
    }
}