package de.crysxd.octoapp.base.usecase

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import javax.inject.Inject

class EmergencyStopUseCase @Inject constructor() : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint) {
        Firebase.analytics.logEvent("emergency_stop_triggered_by_app", Bundle.EMPTY)
        param.createPrinterApi().executeGcodeCommand(GcodeCommand.Single("M112"))
    }
}