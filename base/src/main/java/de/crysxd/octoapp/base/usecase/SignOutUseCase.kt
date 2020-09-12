package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.repository.OctoPrintRepository
import timber.log.Timber
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        octoPrintRepository.clearOctoprintInstanceInformation()
    }
}