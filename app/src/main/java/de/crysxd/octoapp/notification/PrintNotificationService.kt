package de.crysxd.octoapp.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

const val ACTION_STOP = "stop"
const val DISCONNECT_IF_NO_MESSAGE_FOR_MS = 60_000L
const val RETRY_DELAY = 1_000L
const val RETRY_COUNT = 3L

class PrintNotificationService : Service() {

    private val notificationController by lazy { PrintNotificationController.instance }
    private val octoPreferences by lazy { Injector.get().octoPreferences() }
    private val instance by lazy {
        // Instance is fixed for this service. When the active instance is changed the service restarts due to settings change
        Injector.get().octorPrintRepository().getActiveInstanceSnapshot()
    }
    private val eventFlow = Injector.get().octoPrintProvider().eventFlow("notification-service")

    private val coroutineJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineJob + Dispatchers.Main.immediate)
    private var markDisconnectedJob: Job? = null
    private var lastMessageReceivedAt: Long? = null

    private var reconnectionAttempts = 0
    private var notPrintingCounter = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Injector.get().octoPreferences().wasPrintNotificationDisconnected = false
        Injector.get().octoPreferences().wasPrintNotificationPaused = false

        // Start notification
        val instance = instance ?: return let {
            Timber.w("Active instance is null, stopping")
            stop()
        }
        val (notification, notificationId) = runBlocking {
            notificationController.createServiceNotification(instance, "Checking live status...")
        }
        startForeground(notificationId, notification)

        if (PrintNotificationManager.isNotificationEnabled) {
            Timber.i("Creating notification service")

            coroutineScope.launch {
                // Check preconditions
                if (!checkPreconditions()) {
                    Timber.i("Preconditions not met, stopping self")
                    notificationController.clearLast(instance.id)
                    stop()
                } else {
                    Timber.i("Preconditions, allowing connection")
                }

                // Hook into event flow to receive updates
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
            coroutineScope.launch {
                octoPreferences.updatedFlow.collectLatest {
                    if (!octoPreferences.isPrintNotificationEnabled || octoPreferences.activeInstanceId != instance.id) {
                        Timber.i("Settings changed, restarting")
                        PrintNotificationManager.restart(this@PrintNotificationService)
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
        coroutineScope.launch {
            super.onDestroy()

            // Cancel the notification or update it in case we are disconnected or paused (e.g. screen off)
            stopForeground(octoPreferences.wasPrintNotificationDisconnected)
            if (octoPreferences.wasPrintNotificationDisconnected || octoPreferences.wasPrintNotificationPaused) {
                instance?.id?.let { notificationController.update(it, null) }
            }

            PrintNotificationManager.startTime = 0
            ProgressAppWidget.notifyWidgetDataChanged()

            // Last
            coroutineJob.cancel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stop()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun stop() = PrintNotificationManager.stop(this)

    private suspend fun onEventReceived(event: Event) {
        try {
            when (event) {
                is Event.Disconnected -> handleDisconnectedEvent(event)
                is Event.Connected -> handleConnectedEvent()
                is Event.MessageReceived -> (event.message as? Message.CurrentMessage)?.let { handleCurrentMessage(it) }
            }
        } catch (e: Exception) {
            Timber.e(e)
            stop()
        }
    }

    private fun handleDisconnectedEvent(event: Event.Disconnected) {
        ProgressAppWidget.notifyWidgetOffline()
        val minSinceLastMessage = TimeUnit.MILLISECONDS.toMinutes(SystemClock.uptimeMillis() - (lastMessageReceivedAt ?: 0))
        when {
            lastMessageReceivedAt == null && reconnectionAttempts >= 2 -> {
                Timber.w(event.exception, "Unable to connect, stopping self")
                stop()
            }

            minSinceLastMessage >= 0 && reconnectionAttempts >= 3 -> {
                Timber.i("No connection since $minSinceLastMessage min and after $reconnectionAttempts attempts, stopping self with disconnect message")
                Injector.get().octoPreferences().wasPrintNotificationDisconnected = true
                stop()
            }

            else -> {
                Timber.i("No connection since $minSinceLastMessage min, attempting to reconnect")
                reconnectionAttempts++
            }
        }
    }

    private fun handleConnectedEvent() {
        Timber.i("Connected")
        reconnectionAttempts = 0
    }

    private suspend fun handleCurrentMessage(message: Message.CurrentMessage) = instance?.id?.let { instanceId ->
        lastMessageReceivedAt = SystemClock.uptimeMillis()
        ProgressAppWidget.notifyWidgetDataChanged(message)

        // Schedule stop if we don't receive the next message soon
        markDisconnectedJob?.cancel()
        markDisconnectedJob = coroutineScope.launch {
            delay(DISCONNECT_IF_NO_MESSAGE_FOR_MS)
            octoPreferences.wasPrintNotificationDisconnected = true
            stop()
        }

        // Update notification
        if (message.state?.flags?.isPrinting() == true) {
            notPrintingCounter = 0
            // We are printing, update notification
            val print = message.toPrint()
            notificationController.update(instanceId, print)
        } else {
            // We are no longer printing.
            // We need to count the not printing messages because OctoPrint says "not printing" for a short period of time when resuming a print
            if (notPrintingCounter++ >= 3) {
                PrintNotificationManager.stop(this)
            }

            // If the print is done and we saw the print printing in the last state, notify
            notificationController.getLast(instanceId)?.let { last ->
                val current = message.toPrint()
                if (last.objectId == current.objectId) {
                    notificationController.notifyCompleted(instanceId, current)
                }
            }

            notificationController.clearLast(instanceId)
        }
    }

    private fun Message.CurrentMessage.toPrint() = Print(
        objectId = job?.file?.let { "${it.date}+${it.name}" } ?: "unknown",
        fileName = job?.file?.name ?: "unknown",
        source = Print.Source.Live,
        state = state?.flags?.let {
            when {
                it.cancelling -> Print.State.Cancelling
                it.pausing -> Print.State.Pausing
                it.paused -> Print.State.Paused
                else -> null
            }
        } ?: Print.State.Printing,
        sourceTime = Date(),
        appTime = Date(),
        eta = progress?.printTimeLeft?.let { Date(System.currentTimeMillis() + it * 1000) },
        progress = progress?.completion ?: 0f,
    )
}
