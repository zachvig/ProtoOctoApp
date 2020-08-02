package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.crysxd.octoapp.octoprint.OctoPrint
import javax.inject.Inject

class OpenOctoprintWebUseCase @Inject constructor() : UseCase<Pair<OctoPrint, Context>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, Context>) {
        param.first.webUrl.let {
            param.second.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }
}