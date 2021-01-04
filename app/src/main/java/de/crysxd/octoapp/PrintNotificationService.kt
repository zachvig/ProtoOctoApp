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
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import timber.log.Timber

const val ACTION_STOP = "stop"
const val DISCONNECT_IF_NO_MESSAGE_FOR_MS = 60_000L
const val RETRY_DELAY = 1_000L
const val RETRY_COUNT = 3L

class PrintNotificationService : Service() {

    private val coroutineJob = Job()
    private var markDisconnectedJob: Job? = null
    private val eventFlow = Injector.get().octoPrintProvider().eventFlow("notification-service")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        startForeground(notificationId, createInitialNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Destroying notification service")
        notificationManager.cancel(notificationId)
        coroutineJob.cancel()
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
                            // Schedule transition into disconnected state if no message was received for a set timeout
                            markDisconnectedAfterDelay()

                            // Check if still printing
                            val flags = message.state?.flags
                            Timber.v(message.toString())
                            if (flags == null || !listOf(flags.printing, flags.paused, flags.pausing, flags.cancelling).any { it }) {
                                if (message.progress?.completion?.toInt() == maxProgress && didSeePrintBeingActive) {
                                    didSeePrintBeingActive = false
                                    Timber.i("Print done, showing notification")
                                    val name = message.job?.file?.display
                                    notificationManager.notify((3242..4637).random(), createCompletedNotification(name))
                                }

                                Timber.i("Not printing, stopping self")
                                stopSelf()
                                return@let null
                            }

                            // Update notification
                            didSeePrintBeingActive = true
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
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun markDisconnectedAfterDelay() {
        markDisconnectedJob?.cancel()
        markDisconnectedJob = GlobalScope.launch(coroutineJob) {
            delay(DISCONNECT_IF_NO_MESSAGE_FOR_MS)
            notificationManager.notify(notificationId, createDisconnectedNotification())
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
        .setContentTitle(getString(R.string.notification_print_done_title))
        .apply {
            name?.let {
                setContentText(it)
            }
        }
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .build()

    private fun createDisconnectedNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.notification_printing_lost_connection_message))
        .setContentText(lastEta)
        .setProgress(maxProgress, 0, true)
        .addCloseAction()
        .setOngoing(false)
        .setNotificationSilent()
        .build()

    private fun createInitialNotification() = createNotificationBuilder()
        .setContentTitle(getString(R.string.notification_printing_title))
        .setProgress(maxProgress, 0, true)
        .setOngoing(true)
        .addCloseAction()
        .setNotificationSilent()
        .build()

    private fun NotificationCompat.Builder.addCloseAction() = addAction(
        NotificationCompat.Action.Builder(
            null,
            getString(R.string.close),
            PendingIntent.getService(
                this@PrintNotificationService,
                0,
                Intent(this@PrintNotificationService, PrintNotificationService::class.java).setAction(ACTION_STOP),
                0
            )
        ).build()
    )

    private fun createNotificationBuilder() = NotificationCompat.Builder(this, notificationChannelId)
        .setColorized(true)
        .setColor(ContextCompat.getColor(this, R.color.primary_light))
        .setSmallIcon(R.drawable.ic_notification_default)
        .setContentIntent(createStartAppPendingIntent())

    private fun createStartAppPendingIntent() = PendingIntent.getActivity(
        this,
        openAppRequestCode,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

}
