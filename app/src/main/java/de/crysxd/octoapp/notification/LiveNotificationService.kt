package de.crysxd.octoapp.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.data.models.hasPlugin
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_NAME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_TIME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_PROGRESS
import de.crysxd.octoapp.octoprint.models.settings.Settings
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

class LiveNotificationService : Service() {

    companion object {
        const val ACTION_HIBERNATE = "de.crysxd.octoapp.notification.PrintNotificationService.HIBERNATE"
        const val ACTION_WAKE_UP = "de.crysxd.octoapp.notification.PrintNotificationService.WAKE_UP"
        const val DISCONNECT_IF_NO_MESSAGE_FOR_MS = 60_000L
        const val RETRY_DELAY = 2_000L
        const val CHECK_PRINT_ENDED_DELAY = 1_000L
        const val RETRY_COUNT = 3L
    }

    private val notificationController by lazy { PrintNotificationController.instance }
    private val octoPreferences by lazy { BaseInjector.get().octoPreferences() }
    private val instance by lazy {
        // Instance is fixed for this service. When the active instance is changed the service restarts due to settings change
        BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
    }
    private val eventFlow = BaseInjector.get().octoPrintProvider().eventFlow("notification-service")

    private val coroutineJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineJob + Dispatchers.Main.immediate)
    private var markDisconnectedJob: Job? = null
    private var lastMessageReceivedAt: Long? = null
    private var eventFlowJob: Job? = null
    private var checkPrintEndedJob: Job? = null

    private var reconnectionAttempts = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        BaseInjector.get().octoPreferences().wasPrintNotificationDisconnected = false

        // Start notification
        val instance = instance ?: return let {
            Timber.w("Active instance is null, stopping")
            stop()
        }
        val (notification, notificationId) = runBlocking {
            notificationController.createServiceNotification(instance, getString(R.string.print_notification___connecting))
        }
        startForeground(notificationId, notification)

        if (LiveNotificationManager.isNotificationEnabled) {
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

                listenOnEventFlow()
            }

            // Observe changes in preferences
            coroutineScope.launch {
                octoPreferences.updatedFlow.collectLatest {
                    if (!octoPreferences.isLivePrintNotificationsEnabled || octoPreferences.activeInstanceId != instance.id) {
                        Timber.i("Settings changed, restarting")
                        LiveNotificationManager.restart(this@LiveNotificationService)
                    }
                }
            }
        } else {
            Timber.i("Notification service disabled, skipping creation")
            stop()
        }
    }

    private suspend fun checkPreconditions(): Boolean = try {
        if (!LiveNotificationManager.isNotificationEnabled) {
            false
        } else {
            val flags = BaseInjector.get().octoPrintProvider().octoPrint().createPrinterApi().getPrinterState().state?.flags
            val isPrinting = flags?.isPrinting() == true

            // Not printing? Make sure to clear the notification
            if (!isPrinting) {
                instance?.id?.let {
                    notificationController.notifyIdle(it)
                }
            }

            isPrinting
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to check preconditions")
        false
    }

    private fun listenOnEventFlow() {
        // Hook into event flow to receive updates
        eventFlowJob?.cancel()
        reconnectionAttempts = 0
        lastMessageReceivedAt = null
        eventFlowJob = coroutineScope.launch {
            eventFlow.onStart {
                Timber.i("Event flow connected")
            }.onCompletion {
                Timber.i("Event flow disconnected")
            }.onEach {
                onEventReceived(it)
            }.retry(RETRY_COUNT) {
                Timber.e(it, "Fault in event flow. Retrying after ${RETRY_DELAY}ms")
                delay(RETRY_DELAY)
                true
            }.catch {
                Timber.e(it, "Event flow died. Moving into hibernation")
                octoPreferences.wasPrintNotificationDisconnected = true
                hibernate()
            }.collect()
        }
    }

    override fun onDestroy() {
        Timber.i("Service is being destroyed")
        coroutineScope.launch {
            super.onDestroy()

            // Cancel the notification or update it in case we are disconnected or paused (e.g. screen off)
            stopForeground(octoPreferences.wasPrintNotificationDisconnected)
            if (octoPreferences.wasPrintNotificationDisconnected) {
                instance?.id?.let {
                    notificationController.update(instanceId = it, printState = null, stateText = getString(R.string.print_notification___disconnected))
                }
            }

            LiveNotificationManager.startTime = 0
            ProgressAppWidget.notifyWidgetDataChanged()

            // Last
            coroutineJob.cancel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIBERNATE -> {
                eventFlowJob?.cancel()
                Timber.i("Hibernating now")
            }
            ACTION_WAKE_UP -> {
                listenOnEventFlow()
                Timber.i("Woken up")
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun stop() {
        Timber.i("Stopping self")
        LiveNotificationManager.stop(this)
    }

    private fun hibernate() {
        Timber.i("Triggering hibernation")
        LiveNotificationManager.hibernate(this)
    }

    private suspend fun onEventReceived(event: Event) = when (event) {
        is Event.Disconnected -> handleDisconnectedEvent(event)
        is Event.Connected -> handleConnectedEvent()
        is Event.MessageReceived -> (event.message as? Message.CurrentMessage)?.let { handleCurrentMessage(it) }
    }

    private fun handleDisconnectedEvent(event: Event.Disconnected) {
        ProgressAppWidget.notifyWidgetOffline()
        lastMessageReceivedAt = lastMessageReceivedAt ?: SystemClock.uptimeMillis()
        val secsSinceLastMessage = TimeUnit.MILLISECONDS.toSeconds(SystemClock.uptimeMillis() - (lastMessageReceivedAt ?: 0))
        when {
            lastMessageReceivedAt == null && secsSinceLastMessage >= 10 && reconnectionAttempts >= 3 -> {
                Timber.w(event.exception, "Unable to connect within ${secsSinceLastMessage}s and after $reconnectionAttempts attempts, going into hibernation")
                BaseInjector.get().octoPreferences().wasPrintNotificationDisconnected = true
                hibernate()
            }

            secsSinceLastMessage >= 60 && reconnectionAttempts >= 3 -> {
                Timber.i("No connection since ${secsSinceLastMessage}s and after $reconnectionAttempts attempts, going into hibernation")
                BaseInjector.get().octoPreferences().wasPrintNotificationDisconnected = true
                hibernate()
            }

            else -> {
                Timber.i("No connection since ${secsSinceLastMessage}s, attempting to reconnect")
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
            Timber.i("No updates for ${DISCONNECT_IF_NO_MESSAGE_FOR_MS}ms, moving notification to disconnected state")
            notificationController.update(instanceId = instanceId, printState = null, stateText = getString(R.string.print_notification___disconnected))
        }

        // Update notification
        if (message.state?.flags?.isPrinting() == true) {
            checkPrintEndedJob?.cancel()
            checkPrintEndedJob = null
            // We are printing, update notification
            val print = message.toPrint()
            notificationController.update(instanceId, print)
        } else if (checkPrintEndedJob == null) {
            // We are no longer printing.
            // We need double check because OctoPrint says "not printing" for a short period of time when resuming a print
            Timber.i("Print no longer active, checking status in ${CHECK_PRINT_ENDED_DELAY}ms")
            checkPrintEndedJob = coroutineScope.launch {
                try {
                    delay(CHECK_PRINT_ENDED_DELAY)
                    if (checkPreconditions()) {
                        Timber.i("Print still active, resuming")
                    } else {
                        Timber.i("Print ended, winding down")

                        // If the print is done and we saw the print printing in the last state, notify
                        // We don't check for print done events if OctoApp plugin is installed
                        if (instance.hasPlugin(Settings.OctoAppCompanionSettings::class)) {
                            Timber.i("Skipping print completed check, companion configured")
                        } else {
                            notificationController.getLast(instanceId)?.let { last ->
                                val current = message.toPrint()
                                if (last.objectId == current.objectId && current.progress >= 100) {
                                    notificationController.notifyCompleted(instanceId, current)
                                }
                            }
                        }

                        notificationController.clearLast(instanceId)
                        stop()
                    }
                } finally {
                    checkPrintEndedJob = null
                }
            }
        }
    }

    private fun Message.CurrentMessage.toPrint() = PrintState(
        fileDate = job?.file?.date ?: DEFAULT_FILE_TIME,
        fileName = job?.file?.name ?: DEFAULT_FILE_NAME,
        source = PrintState.Source.Live,
        state = state?.flags?.let {
            when {
                it.cancelling -> PrintState.State.Cancelling
                it.pausing -> PrintState.State.Pausing
                it.paused -> PrintState.State.Paused
                else -> null
            }
        } ?: PrintState.State.Printing,
        sourceTime = Date(),
        appTime = Date(),
        eta = progress?.printTimeLeft?.let { Date(System.currentTimeMillis() + it * 1000) },
        progress = progress?.completion ?: DEFAULT_PROGRESS,
    )
}
