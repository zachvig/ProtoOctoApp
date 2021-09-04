package de.crysxd.octoapp.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

const val ACTION_STOP = "stop"
const val DISCONNECT_IF_NO_MESSAGE_FOR_MS = 60_000L
const val RETRY_DELAY = 1_000L
const val RETRY_COUNT = 3L
const val MAX_PROGRESS = 100
const val FILAMENT_CHANGE_NOTIFICATION_ID = 3100
const val NOTIFICATION_ID = 2999

class PrintNotificationService : Service() {

    private val coroutineJob = Job()
    private var markDisconnectedJob: Job? = null
    private val eventFlow = Injector.get().octoPrintProvider().eventFlow("notification-service")
    private val notificationFactory by lazy { PrintNotificationFactory(this) }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val formatEtaUseCase = Injector.get().formatEtaUseCase()
    private var didSeePrintBeingActive = false
    private var didSeeFilamentChangeAt = 0L
    private var pausedBecauseOfFilamentChange = false
    private var notPrintingCounter = 0
    private var lastMessageReceivedAt: Long? = null
    private var reconnectionAttempts = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Injector.get().octoPreferences().wasPrintNotificationDisconnected = false
        Injector.get().octoPreferences().wasPrintNotificationPaused = false

        // Register notification channel
        notificationFactory.createNotificationChannels()

        // Start notification
        startForeground(NOTIFICATION_ID, notificationFactory.createInitialNotification())

        if (PrintNotificationManager.isNotificationEnabled) {
            Timber.i("Creating notification service")

            // Check preconditions
            AppScope.launch(coroutineJob) {
                if (!checkPreconditions()) {
                    Timber.i("Preconditions not met, stopping self")
                    stop()
                } else {
                    Timber.i("Preconditions, allowing connection")
                }
            }

            // Hook into event flow to receive updates
            AppScope.launch(coroutineJob) {
                eventFlow.onEach {
                    onEventReceived(it)
                }.retry(RETRY_COUNT) {
                    delay(RETRY_DELAY)
                    true
                }.catch {
                    Timber.e(it)
                }.collect()
            }

            // Observe changes in preferences
            AppScope.launch(coroutineJob) {
                Injector.get().octoPreferences().updatedFlow.collectLatest {
                    if (!PrintNotificationManager.isNotificationEnabled) {
                        Timber.i("Service disabled, stopping self")
                        stop()
                    }
                }
            }
        } else {
            Timber.i("Notification service disabled, skipping creation")
            stop()
        }
    }

    private suspend fun checkPreconditions(): Boolean = try {
        if (!PrintNotificationManager.isNotificationEnabled) {
            false
        } else {
            val flags = Injector.get().octoPrintProvider().octoPrint().createPrinterApi().getPrinterState().state?.flags
            flags?.isPrinting() == true
        }
    } catch (e: Exception) {
        Timber.e(e)
        false
    }

    override fun onDestroy() {
        super.onDestroy()
        val paused = Injector.get().octoPreferences().wasPrintNotificationPaused
        val disconnected = Injector.get().octoPreferences().wasPrintNotificationDisconnected
        Timber.i("Destroying notification service (was disconnected=$disconnected paused=$paused)")
        stopForeground(false)
        when {
            disconnected -> notificationManager.notify(NOTIFICATION_ID, notificationFactory.createDisconnectedNotification())
            paused -> notificationManager.notify(NOTIFICATION_ID, notificationFactory.creareReconnectingNotification())
            else -> notificationManager.cancel(NOTIFICATION_ID)
        }
        coroutineJob.cancel()
        PrintNotificationManager.startTime = 0
        ProgressAppWidget.notifyWidgetDataChanged()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stop()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun stop() {
        PrintNotificationManager.stop(this)
    }

    private suspend fun onEventReceived(event: Event) {
        try {
            when (event) {
                is Event.Disconnected -> {
                    ProgressAppWidget.notifyWidgetOffline()
                    val minSinceLastMessage = TimeUnit.MILLISECONDS.toMinutes(SystemClock.uptimeMillis() - (lastMessageReceivedAt ?: 0))
                    when {
                        lastMessageReceivedAt == null && reconnectionAttempts >= 2 -> {
                            Timber.w(event.exception, "Unable to connect, stopping self")
                            stop()
                            null
                        }

                        minSinceLastMessage >= 2 && reconnectionAttempts >= 3 -> {
                            Timber.i("No connection since $minSinceLastMessage min and after $reconnectionAttempts attempts, stopping self with disconnect message")
                            Injector.get().octoPreferences().wasPrintNotificationDisconnected = true
                            stop()
                            notificationFactory.createDisconnectedNotification()
                        }

                        else -> {
                            Timber.i("No connection since $minSinceLastMessage min, attempting to reconnect")
                            reconnectionAttempts++
                            notificationFactory.creareReconnectingNotification()
                        }
                    }
                }

                is Event.Connected -> {
                    Timber.i("Connected")
                    reconnectionAttempts = 0
                    notificationFactory.createInitialNotification()
                }

                is Event.MessageReceived -> {
                    (event.message as? Message.CurrentMessage)?.let { message ->
                        lastMessageReceivedAt = SystemClock.uptimeMillis()
                        ProgressAppWidget.notifyWidgetDataChanged(message)
                        updateFilamentChangeNotification(message)
                        updatePrintNotification(message)
                    }
                }
                else -> null
            }?.let {
                notificationManager.notify(NOTIFICATION_ID, it)
            }
        } catch (e: Exception) {
            Timber.e(e)
            stop()
        }
    }

    private fun updateFilamentChangeNotification(message: Message.CurrentMessage) {
        if (message.logs.any { it.contains("M600") }) {
            didSeeFilamentChangeAt = SystemClock.uptimeMillis()
            notificationManager.notify(FILAMENT_CHANGE_NOTIFICATION_ID, notificationFactory.createFilamentChangeNotification())
        }
    }

    private suspend fun updatePrintNotification(message: Message.CurrentMessage): Notification? {
        // Schedule transition into disconnected state if no message was received for a set timeout
        markDisconnectedAfterDelay()

        // Check if still printing
        val flags = message.state?.flags
        if (flags == null || !flags.isPrinting()) {
            // OctoPrint sometimes reports not printing when we resume a print but only for a split second.
            // We need to count the updates with not printing before exiting the service
            // We immediately quit if null, print is completed or closedOrError
            notPrintingCounter++
            val printDone = message.progress?.completion?.toInt() == MAX_PROGRESS
            if (flags == null || notPrintingCounter > 3 || flags.closedOrError || printDone) {
                if (printDone && didSeePrintBeingActive) {
                    didSeePrintBeingActive = false
                    Timber.i("Print done, showing notification")
                    val name = message.job?.file?.display
                    notificationManager.notify((3000..3500).random(), notificationFactory.createCompletedNotification(name))
                }

                Timber.i("Not printing, stopping self")
                stop()
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
            val smartEta = formatEtaUseCase.execute(FormatEtaUseCase.Params(leftSecs, allowRelative = true))
            notificationFactory.lastEta = formatEtaUseCase.execute(FormatEtaUseCase.Params(leftSecs, allowRelative = false))

            val detail = getString(R.string.print_notification___printing_message, progress, smartEta)
            val title = getString(
                when {
                    flags.pausing -> R.string.print_notification___pausing_title
                    flags.paused -> {
                        // If we are paused and we saw a filament change command just before,
                        // we assume we where paused because of the filament change
                        if ((SystemClock.uptimeMillis() - didSeeFilamentChangeAt) < 10000) {
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

            return notificationFactory.createProgressNotification(progress, title, detail)
        }

        return null
    }

    private fun markDisconnectedAfterDelay() {
        markDisconnectedJob?.cancel()
        markDisconnectedJob = AppScope.launch(coroutineJob) {
            delay(DISCONNECT_IF_NO_MESSAGE_FOR_MS)
            notificationManager.notify(NOTIFICATION_ID, notificationFactory.creareReconnectingNotification())
        }
    }
}
