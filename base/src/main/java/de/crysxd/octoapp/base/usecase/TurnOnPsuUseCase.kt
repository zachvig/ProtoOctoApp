package de.crysxd.octoapp.base.usecase

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.OctoPrintProvider
import timber.log.Timber
import javax.inject.Inject

class TurnOnPsuUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        Firebase.analytics.logEvent("psu_turned_on", Bundle.EMPTY)
        octoPrintProvider.octoPrint().createPsuApi().turnPsuOn()
    }
}