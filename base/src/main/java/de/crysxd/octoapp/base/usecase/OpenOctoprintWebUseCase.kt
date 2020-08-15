package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.crysxd.octoapp.octoprint.OctoPrint
import timber.log.Timber
import javax.inject.Inject

class OpenOctoprintWebUseCase @Inject constructor() : UseCase<Pair<OctoPrint, Context>, Unit>() {

    override suspend fun doExecute(param: Pair<OctoPrint, Context>, timber: Timber.Tree) {
        param.first.webUrl.let {
            param.second.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }
}