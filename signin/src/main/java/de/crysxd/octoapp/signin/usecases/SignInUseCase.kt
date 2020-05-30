package de.crysxd.octoapp.signin.usecases

import de.crysxd.octoapp.base.OctoPrintRepository
import de.crysxd.octoapp.base.UseCase
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.signin.models.SignInInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Exception


class SignInUseCase(private val octoprintRepository: OctoPrintRepository) :
    UseCase<SignInInformation, Boolean> {

    override suspend fun execute(param: SignInInformation): Boolean = withContext(Dispatchers.IO) {
        val octoprintInstanceInformation = OctoPrintInstanceInformation(
            param.ipAddress,
            param.port.toInt(),
            param.ipAddress
        )

        val octoprint = octoprintRepository.getOctoprint(octoprintInstanceInformation)

        try {
            // Test connection, will throw in case of faulty configuration
            val version = octoprint.createVersionApi().getVersion()
            Timber.i("Connected to ${version.serverVersionText}")
            octoprintRepository.storeOctoprintInstanceInformation(octoprintInstanceInformation)
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }
}