package de.crysxd.octoapp.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.colorTheme

class PrintNotificationFactory(context: Context) : ContextWrapper(context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val normalNotificationChannelId = "print_progress"
    private val filamentNotificationChannelId = "filament_change"
    private val openAppRequestCode = 3249
    private val maxProgress = 100
    var lastEta: String = ""

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    normalNotificationChannelId,
                    getString(R.string.notification_channel___print_progress),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )


            val soundUri = Uri.parse("android.resource://${applicationContext.packageName}/${R.raw.notification_filament_change}")
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    filamentNotificationChannelId,
                    getString(R.string.notification_channel_filament_change),
                    NotificationManager.IMPORTANCE_HIGH
                ).also {
                    it.setSound(soundUri, audioAttributes)
                }
            )
        }
    }

    fun createProgressNotification(progress: Int, title: String, status: String) = createNotificationBuilder()
        .setContentTitle(title)
        .setContentText(status)
        .setProgress(maxProgress, progress, false)
        .setOngoing(true)
        .addCloseAction()
        .setNotificationSilent()
        .build()

    fun createCompletedNotification(name: String?) = createNotificationBuilder()
        .setContentTitle(getString(R.string.print_notification___print_done_title))
        .apply {
            name?.let {
                setContentText(it)
            }
        }
        .setDefaults(Notification.DEFAULT_SOUND)
        .setDefaults(Notification.DEFAULT_VIBRATE)
        .build()

    fun createFilamentChangeNotification() = createNotificationBuilder(filamentNotificationChannelId)
        .setContentTitle(getString(R.string.print_notification___filament_change_required))
        .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
        .setDefaults(Notification.DEFAULT_VIBRATE)
        .build()

    fun creareReconnectingNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.print_notification___reconnecting_title))
        .setContentText(lastEta)
        .setProgress(maxProgress, 0, true)
        .addCloseAction()
        .setOngoing(false)
        .setNotificationSilent()
        .build()

    fun createDisconnectedNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.print_notification___disconnected_title))
        .setContentText(getString(R.string.print_notification___disconnected_message, lastEta))
        .setOngoing(false)
        .setNotificationSilent()
        .setAutoCancel(true)
        .build()

    fun createInitialNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.print_notification___reconnecting_title))
        .setProgress(maxProgress, 0, true)
        .setOngoing(true)
        .addCloseAction()
        .setNotificationSilent()
        .build()

    fun NotificationCompat.Builder.addCloseAction() = addAction(
        NotificationCompat.Action.Builder(
            null,
            getString(R.string.print_notification___close),
            PendingIntent.getService(
                this@PrintNotificationFactory,
                0,
                Intent(this@PrintNotificationFactory, PrintNotificationService::class.java).setAction(ACTION_STOP),
                0
            )
        ).build()
    )

    fun createNotificationBuilder(notificationChannelId: String = normalNotificationChannelId) = NotificationCompat.Builder(this, notificationChannelId)
        .setColorized(true)
        .setColor(Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.colorTheme?.light ?: Color.WHITE)
        .setSmallIcon(R.drawable.ic_notification_default)
        .setContentIntent(createStartAppPendingIntent())

    fun createStartAppPendingIntent() = PendingIntent.getActivity(
        this,
        openAppRequestCode,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}