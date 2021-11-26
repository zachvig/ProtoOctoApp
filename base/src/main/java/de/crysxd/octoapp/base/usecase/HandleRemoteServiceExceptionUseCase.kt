package de.crysxd.octoapp.base.usecase

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.data.repository.NotificationIdRepository
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.utils.ExceptionReceivers
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.exceptions.RemoteServiceConnectionBrokenException
import timber.log.Timber
import javax.inject.Inject

class HandleRemoteServiceException @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val context: Context,
    private val notificationIdRepository: NotificationIdRepository,
) : UseCase<Exception, Unit>() {

    override suspend fun doExecute(param: Exception, timber: Timber.Tree) {
        if (param is RemoteServiceConnectionBrokenException && param is OctoPrintException) {
            octoPrintRepository.updateActive {
                it.copy(alternativeWebUrl = null, octoEverywhereConnection = null)
            }

            if (!ExceptionReceivers.dispatchException(param)) {
                showErrorNotification(param, param.remoteServiceName)
            }
        }
    }

    private fun showErrorNotification(e: OctoPrintException, serviceName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = "Issue with $serviceName"
        val notification = NotificationCompat.Builder(context, context.getString(R.string.updates_notification_channel))
            .setContentTitle(title)
            .setContentText(e.userFacingMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.userFacingMessage))
            .setSmallIcon(R.drawable.ic_notification_default)
            .setTicker(title)
            .build()
        manager.notify(notificationIdRepository.nextUpdateNotificationId(), notification)
    }
}