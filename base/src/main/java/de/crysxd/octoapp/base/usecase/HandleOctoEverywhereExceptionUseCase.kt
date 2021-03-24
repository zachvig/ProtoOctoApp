package de.crysxd.octoapp.base.usecase

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.octoprint.exceptions.OctoEverywhereConnectionNotFoundException
import de.crysxd.octoapp.octoprint.exceptions.OctoEverywhereSubscriptionMissingException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

class HandleOctoEverywhereExceptionUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val context: Context,
) : UseCase<Exception, Unit>() {

    override suspend fun doExecute(param: Exception, timber: Timber.Tree) {
        if (param is OctoEverywhereSubscriptionMissingException || param is OctoEverywhereConnectionNotFoundException) {
            octoPrintRepository.updateActive {
                it.copy(alternativeWebUrl = null, octoEverywhereConnection = null)
            }
            OctoActivity.instance?.showDialog(param) ?: showErrorNotification(param as OctoPrintException)
        }
    }

    private fun showErrorNotification(e: OctoPrintException) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = "Issue with OctoEverywhere"
        val notification = NotificationCompat.Builder(context, context.getString(R.string.updates_notification_channel))
            .setContentTitle(title)
            .setContentText(e.userFacingMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.userFacingMessage))
            .setSmallIcon(R.drawable.ic_notification_default)
            .setTicker(title)
            .build()
        manager.notify(8223, notification)
    }
}