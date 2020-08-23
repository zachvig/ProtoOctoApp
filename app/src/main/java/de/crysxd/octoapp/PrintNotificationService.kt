package de.crysxd.octoapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

const val ACTION_STOP = "stop"

class PrintNotificationService : Service() {

    private val observer = Observer(this::onEventReceived)
    private val liveData = Injector.get().octoPrintProvider().eventLiveData
    private val openAppRequestCode = 3249
    private val maxProgress = 100
    private val notificationId = 3249
    private val notificationChannelId = "print_progress"
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val formatDurationUseCase: FormatDurationUseCase = Injector.get().formatDurationUseCase()
    private var lastEta: String = ""
    private var didSeePrintBeingActive = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("Creating notification service")
        liveData.observeForever(observer)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        startForeground(notificationId, createInitialNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Destroying notification service")
        notificationManager.cancel(notificationId)
        liveData.removeObserver(observer)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun onEventReceived(event: Event) {
        GlobalScope.launch {
            try {
                when (event) {
                    is Event.Disconnected -> createDisconnectedNotification()
                    is Event.Connected -> createInitialNotification()
                    is Event.MessageReceived -> {
                        (event.message as? Message.CurrentMessage)?.let { message ->
                            // Check if still printing
                            val flags = message.state?.flags
                            Timber.v(message.toString())
                            if (flags == null || !listOf(flags.printing, flags.paused, flags.pausing, flags.cancelling).any { it }) {
                                if (message.progress?.completion?.toInt() == maxProgress && didSeePrintBeingActive) {
                                    didSeePrintBeingActive = false
                                    Timber.i("Print done, showing notification")
                                    notificationManager.notify((3242..4637).random(), createCompletedNotification())
                                } else {
                                    didSeePrintBeingActive = true
                                }

                                Timber.i("Not printing, stopping self")
                                stopSelf()
                                return@let null
                            }

                            // Update notification
                            message.progress?.let {
                                val progress = it.completion.toInt()
                                val left = formatDurationUseCase.execute(it.printTimeLeft.toLong())

                                lastEta = getString(R.string.print_eta_x, Injector.get().formatEtaUseCase().execute(it.printTimeLeft))
                                val detail = getString(R.string.notification_printing_message, progress, left)
                                val title = getString(
                                    when {
                                        flags.pausing -> R.string.notification_pausing_title
                                        flags.paused -> R.string.notification_paused_title
                                        flags.cancelling -> R.string.notification_cancelling_title
                                        else -> R.string.notification_printing_title
                                    }
                                )

                                createProgressNotification(progress, title, detail)
                            }
                        }
                    }
                    else -> null
                }?.let {
                    Timber.d("Updating notification")
                    notificationManager.notify(notificationId, it)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                notificationChannelId,
                getString(R.string.notification_channel_print_progress),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun createProgressNotification(progress: Int, title: String, status: String) = createNotificationBuilder()
        .setContentTitle(title)
        .setContentText(status)
        .setProgress(maxProgress, progress, false)
        .build()

    private fun createCompletedNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.notification_print_done_title))
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .setOngoing(false)
        .build()

    private fun createDisconnectedNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.notification_printing_lost_connection_message))
        .setContentText(lastEta)
        .setProgress(maxProgress, 0, true)
        .addAction(
            NotificationCompat.Action.Builder(
                null,
                getString(R.string.close),
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, PrintNotificationService::class.java).setAction(ACTION_STOP),
                    0
                )
            ).build()
        )
        .setOngoing(false)
        .build()

    private fun createInitialNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.notification_printing_title))
        .setProgress(maxProgress, 0, true)
        .build()

    private fun createNotificationBuilder() = NotificationCompat.Builder(this, notificationChannelId)
        .setOngoing(true)
        .setColorized(true)
        .setColor(ContextCompat.getColor(this, R.color.primary_light))
        .setSmallIcon(R.drawable.ic_notification_default)
        .setContentIntent(createStartAppPendingIntent())
        .setNotificationSilent()


    private fun createStartAppPendingIntent() = PendingIntent.getActivity(
        this,
        openAppRequestCode,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

}
