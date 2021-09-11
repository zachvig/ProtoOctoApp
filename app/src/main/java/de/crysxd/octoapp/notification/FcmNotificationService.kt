package de.crysxd.octoapp.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_NAME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_TIME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_PROGRESS
import de.crysxd.octoapp.widgets.createLaunchAppIntent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

class FcmNotificationService : FirebaseMessagingService() {

    private val notificationController by lazy { PrintNotificationController.instance }
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("New token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Timber.i("Received message")
        message.data["raw"]?.let {
            handleRawDataEvent(it, Date(message.sentTime))
        }

        message.notification?.let {
            handleNotification(it)
        }
    }

    private fun handleNotification(notification: RemoteMessage.Notification) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getString(R.string.updates_notification_channel)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentText(notification.body)
            .setContentTitle(notification.title)
            .setSmallIcon(R.drawable.ic_notification_default)
            .setAutoCancel(true)
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.primary_dark))
            .setContentIntent(createLaunchAppIntent(this, null))
        manager.notify(Injector.get().notificationIdRepository().nextUpdateNotificationId(), notificationBuilder.build())
    }

    private fun handleRawDataEvent(raw: String, sentTime: Date) = AppScope.launch(exceptionHandler) {
        Timber.i("Received message with raw data: $raw")
        val data = Gson().fromJson(raw, FcmPrintEvent::class.java)

        when (data.type) {
            FcmPrintEvent.Type.Completed -> notificationController.notifyCompleted(
                instanceId = data.instanceId,
                printState = data.toPrintState(sentTime)
            )

            FcmPrintEvent.Type.FilamentRequired -> notificationController.notifyFilamentRequired(
                instanceId = data.instanceId,
                printState = data.toPrintState(sentTime)
            )

            FcmPrintEvent.Type.Idle -> notificationController.notifyIdle(
                instanceId = data.instanceId
            )

            FcmPrintEvent.Type.Paused,
            FcmPrintEvent.Type.Printing -> {
                notificationController.update(
                    instanceId = data.instanceId,
                    printState = data.toPrintState(sentTime)
                )
            }
        }
    }

    private fun FcmPrintEvent.toPrintState(sentTime: Date) = PrintState(
        source = PrintState.Source.Remote,
        progress = progress ?: DEFAULT_PROGRESS,
        appTime = Date(),
        sourceTime = sentTime,
        fileName = fileName ?: DEFAULT_FILE_NAME,
        fileDate = fileDate ?: DEFAULT_FILE_TIME,
        eta = timeLeft?.let { Date(sentTime.time + timeLeft * 1000) },
        state = when (type) {
            FcmPrintEvent.Type.Printing -> PrintState.State.Printing
            FcmPrintEvent.Type.Paused -> PrintState.State.Paused
            FcmPrintEvent.Type.FilamentRequired -> PrintState.State.Paused
            FcmPrintEvent.Type.Completed -> PrintState.State.Idle
            FcmPrintEvent.Type.Idle -> PrintState.State.Idle
        }
    )
}