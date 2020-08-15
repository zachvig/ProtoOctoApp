package de.crysxd.octoapp.base.usecase

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.octoprint.OctoPrint
import timber.log.Timber
import javax.inject.Inject

class TurnOnPsuUseCase @Inject constructor() : UseCase<OctoPrint, Unit>() {

    override suspend fun doExecute(param: OctoPrint, timber: Timber.Tree) {
        Firebase.analytics.logEvent("psu_turned_on", Bundle.EMPTY)
        param.createPsuApi().turnPsuOn()
    }
}