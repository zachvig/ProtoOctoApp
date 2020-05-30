package de.crysxd.octoapp.signin.usecases

import de.crysxd.octoapp.base.OctoPrintRepository
import de.crysxd.octoapp.base.UseCase
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.signin.models.SignInInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber


class SignInUseCase(private val octoprintRepository: OctoPrintRepository) : UseCase<SignInInformation, Unit> {

    override suspend fun execute(param: SignInInformation) = withContext(Dispatchers.IO) {
        val octoprintInstanceInformation = OctoPrintInstanceInformation(
            param.ipAddress,
            param.port.toInt(),
            param.ipAddress
        )

        val octoprint = octoprintRepository.getOctoprint(octoprintInstanceInformation)

        // Test connection, will throw in case of faulty configuration
        val version = octoprint.createVersionApi().getVersion()
        Timber.i("Connected to ${version.serverVersionText}")

        octoprintRepository.storeOctoprintInstanceInformation(octoprintInstanceInformation)
    }
}