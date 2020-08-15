package de.crysxd.octoapp.base.usecase

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject

class EmergencyStopUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        Firebase.analytics.logEvent("emergency_stop_triggered_by_app", Bundle.EMPTY)
        octoPrintProvider.octoPrint().createPrinterApi().executeGcodeCommand(GcodeCommand.Single("M112"))
    }
}