package de.crysxd.octoapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.colorTheme
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

const val ACTION_STOP = "stop"
const val DISCONNECT_IF_NO_MESSAGE_FOR_MS = 60_000L
const val RETRY_DELAY = 1_000L
const val RETRY_COUNT = 3L
const val FILAMENT_CHANGE_NOTIFICATION_ID = 3100

class PrintNotificationService : Service() {

    companion object {
        const val NOTIFICATION_ID = 3249
        private val isNotificationEnabled get() = Injector.get().octoPreferences().isPrintNotificationEnabled

        fun start(context: Context) {
            if (isNotificationEnabled) {
                val intent = Intent(context, PrintNotificationService::class.java)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PrintNotificationService::class.java)
            context.stopService(intent)
        }
    }

    private val coroutineJob = Job()
    private var markDisconnectedJob: Job? = null
    private val eventFlow = Injector.get().octoPrintProvider().eventFlow("notification-service")
    private val openAppRequestCode = 3249
    private val maxProgress = 100
    private val normalNotificationChannelId = "print_progress"
    private val filamentNotificationChannelId = "filament_change"
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val formatEtaUseCase = Injector.get().formatEtaUseCase()
    private var lastEta: String = ""
    private var didSeePrintBeingActive = false
    private var didSeeFilamentChangeAt = 0L
    private var pausedBecauseOfFilamentChange = false
    private var notPrintingCounter = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (isNotificationEnabled) {
            Timber.i("Creating notification service")
            GlobalScope.launch(coroutineJob) {
                eventFlow.onEach {
                    onEventReceived(it)
                }.retry(RETRY_COUNT) {
                    delay(RETRY_DELAY)
                    true
                }.catch {
                    Timber.e(it)
                }.collect()
            }

            GlobalScope.launch(coroutineJob) {
                Injector.get().octoPreferences().updatedFlow.collectLatest {
                    if (!isNotificationEnabled) {
                        Timber.i("Service disabled, stopping self")
                        stopSelf()
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannels()
            }

            startForeground(NOTIFICATION_ID, createInitialNotification())
        } else {
            Timber.i("Notification service disabled, skipping creation")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Destroying notification service")
        notificationManager.cancel(NOTIFICATION_ID)
        coroutineJob.cancel()
        ProgressAppWidget.notifyWidgetDataChanged()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun onEventReceived(event: Event) {
        try {
            when (event) {
                is Event.Disconnected -> {
                    ProgressAppWidget.notifyWidgetOffline()
                    createDisconnectedNotification()
                }
                is Event.Connected -> createInitialNotification()
                is Event.MessageReceived -> {
                    (event.message as? Message.CurrentMessage)?.let { message ->
                        ProgressAppWidget.notifyWidgetDataChanged(message)
                        updateFilamentChangeNotification(message)
                        updatePrintNotification(message)
                    }
                }
                else -> null
            }?.let {
                Timber.v("Updating notification")
                notificationManager.notify(NOTIFICATION_ID, it)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun updateFilamentChangeNotification(message: Message.CurrentMessage) {
        if (message.logs.any { it.contains("M600") }) {
            didSeeFilamentChangeAt = System.currentTimeMillis()
            notificationManager.notify(FILAMENT_CHANGE_NOTIFICATION_ID, createFilamentChangeNotification())
        }
    }

    private suspend fun updatePrintNotification(message: Message.CurrentMessage): Notification? {
        // Schedule transition into disconnected state if no message was received for a set timeout
        markDisconnectedAfterDelay()

        // Check if still printing
        val flags = message.state?.flags
        Timber.v(message.toString())
        if (flags == null || !listOf(flags.printing, flags.paused, flags.pausing, flags.cancelling).any { it }) {
            // OctoPrint sometimes reports not printing when we resume a print but only for a split second.
            // We need to count the updates with not printing before exiting the service
            notPrintingCounter++
            if (flags == null || notPrintingCounter > 3) {
                if (message.progress?.completion?.toInt() == maxProgress && didSeePrintBeingActive) {
                    didSeePrintBeingActive = false
                    Timber.i("Print done, showing notification")
                    val name = message.job?.file?.display
                    notificationManager.notify((3242..4637).random(), createCompletedNotification(name))
                }

                Timber.i("Not printing, stopping self")
                stopSelf()
                return null
            }
        } else {
            notPrintingCounter = 0
        }

        // Update notification
        didSeePrintBeingActive = true
        message.progress?.let {
            val leftSecs = it.printTimeLeft.toLong()
            val progress = it.completion.toInt()
            val smartEta = formatEtaUseCase.execute(FormatEtaUseCase.Params(leftSecs, allowRelative = false))
            lastEta = formatEtaUseCase.execute(FormatEtaUseCase.Params(leftSecs, allowRelative = false))

            val detail = getString(R.string.print_notification___printing_message, progress, smartEta)
            val title = getString(
                when {
                    flags.pausing -> R.string.print_notification___pausing_title
                    flags.paused -> {
                        // If we are paused and we saw a filament change command just before,
                        // we assume we where paused because of the filament change
                        if ((System.currentTimeMillis() - didSeeFilamentChangeAt) < 10000) {
                            pausedBecauseOfFilamentChange = true
                        }

                        if (pausedBecauseOfFilamentChange) {
                            R.string.print_notification___paused_filamet_change_title
                        } else {
                            R.string.print_notification___paused_title
                        }
                    }
                    flags.cancelling -> R.string.print_notification___cancelling_title
                    else -> {
                        pausedBecauseOfFilamentChange = false
                        notificationManager.cancel(FILAMENT_CHANGE_NOTIFICATION_ID)
                        R.string.print_notification___printing_title
                    }
                }
            )

            return createProgressNotification(progress, title, detail)
        }

        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
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

    private fun markDisconnectedAfterDelay() {
        markDisconnectedJob?.cancel()
        markDisconnectedJob = GlobalScope.launch(coroutineJob) {
            delay(DISCONNECT_IF_NO_MESSAGE_FOR_MS)
            notificationManager.notify(NOTIFICATION_ID, createDisconnectedNotification())
        }
    }

    private fun createProgressNotification(progress: Int, title: String, status: String) = createNotificationBuilder()
        .setContentTitle(title)
        .setContentText(status)
        .setProgress(maxProgress, progress, false)
        .setOngoing(true)
        .addCloseAction()
        .setNotificationSilent()
        .build()

    private fun createCompletedNotification(name: String?) = createNotificationBuilder()
        .setContentTitle(getString(R.string.print_notification___print_done_title))
        .apply {
            name?.let {
                setContentText(it)
            }
        }
        .setDefaults(Notification.DEFAULT_SOUND)
        .setDefaults(Notification.DEFAULT_VIBRATE)
        .build()

    private fun createFilamentChangeNotification() = createNotificationBuilder(filamentNotificationChannelId)
        .setContentTitle(getString(R.string.print_notification___filament_change_required))
        .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
        .setDefaults(Notification.DEFAULT_VIBRATE)
        .build()

    private fun createDisconnectedNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.print_notification___printing_lost_connection_message))
        .setContentText(lastEta)
        .setProgress(maxProgress, 0, true)
        .addCloseAction()
        .setOngoing(false)
        .setNotificationSilent()
        .build()

    private fun createInitialNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.print_notification___printing_title))
        .setProgress(maxProgress, 0, true)
        .setOngoing(true)
        .addCloseAction()
        .setNotificationSilent()
        .build()

    private fun NotificationCompat.Builder.addCloseAction() = addAction(
        NotificationCompat.Action.Builder(
            null,
            getString(R.string.print_notification___close),
            PendingIntent.getService(
                this@PrintNotificationService,
                0,
                Intent(this@PrintNotificationService, PrintNotificationService::class.java).setAction(ACTION_STOP),
                0
            )
        ).build()
    )

    private fun createNotificationBuilder(notificationChannelId: String = normalNotificationChannelId) = NotificationCompat.Builder(this, notificationChannelId)
        .setColorized(true)
        .setColor(Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.colorTheme?.light ?: Color.WHITE)
        .setSmallIcon(R.drawable.ic_notification_default)
        .setContentIntent(createStartAppPendingIntent())

    private fun createStartAppPendingIntent() = PendingIntent.getActivity(
        this,
        openAppRequestCode,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

}
