package de.crysxd.octoapp.notification

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import timber.log.Timber
import java.util.Date

class NotificationController(
    private val notificationFactory: PrintNotificationFactory2,
    context: Context
) : ContextWrapper(context) {

    companion object {
        private val PRINT_STATUS_NOTIFICATION_ID_RANGE = 10_000..14_999
        private val PRINT_EVENT_NOTIFICATION_ID_RANGE = 15_000..15_999
    }

    private val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationFactory.createNotificationChannels()
        }
    }

    fun createServiceNotification(statusText: String) = notificationFactory.createServiceNotification(statusText)

    suspend fun update(instanceId: String, print: Print) {
        val last = getLast(instanceId)
        val proceed = when {
            // New live events always proceed
            print.source == Print.Source.Live -> true

            // If the last was from remote and this is from remote (implied) and the source time progressed, then we proceed
            last.source == Print.Source.Remote && print.sourceTime > last.sourceTime -> true

            // If the last was live but is outdated, then we proceed
            last.source == Print.Source.Live && last.sourceTime.before(liveThreshold) -> true

            // Else: we drop
            else -> false
        }

        if (proceed) {
            setLast(instanceId, print)
            val notificationId = getNotificationId(instanceId, PRINT_STATUS_NOTIFICATION_ID_RANGE)
            notificationFactory.createStatusNotification(instanceId, print)?.let {
                notificationManager.notify(notificationId, it)
            } ?: Timber.e(IllegalStateException("Received update event for instance $instanceId but instance was not found"))
        }
    }

    fun notifyCompleted(instanceId: String, print: Print) = notificationFactory.createPrintCompletedNotification(
        instanceId = instanceId,
        print = print
    )?.let {
        notificationManager.notify(nextEventNotificationId(), it)
    } ?: Timber.e(IllegalStateException("Received completed event for instance $instanceId but instance was not found"))

    fun notifyFilamentRequired(instanceId: String, print: Print) = notificationFactory.createFilamentChangeNotification(
        instanceId = instanceId,
    )?.let {
        notificationManager.notify(nextEventNotificationId(), it)
    } ?: Timber.e(IllegalStateException("Received filament event for instance $instanceId but instance was not found"))

    private fun getLast(instanceId: String): Print = TODO()

    private fun setLast(instanceId: String, print: Print): Unit = TODO()

    private val liveThreshold get() = Date(System.currentTimeMillis() - 30_000)

    private fun nextEventNotificationId(): Int = TODO()

}