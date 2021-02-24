package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import timber.log.Timber
import javax.inject.Inject

class OpenOctoprintWebUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Context, Unit>() {

    override suspend fun doExecute(param: Context, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().webUrl.let { webUrl ->
            param.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}