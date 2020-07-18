package de.crysxd.octoapp.base.usecase

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import javax.inject.Inject

class CancelPrintJobUseCase @Inject constructor() : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint) {
        Firebase.analytics.logEvent("print_canceled_by_app", Bundle.EMPTY)
        param.createJobApi().executeJobCommand(JobCommand.CancelJobCommand)
    }
}