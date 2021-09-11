package de.crysxd.octoapp.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.repository.NotificationIdRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import timber.log.Timber
import java.util.Date

class PrintNotificationController(
    private val notificationFactory: PrintNotificationFactory,
    private val printNotificationIdRepository: NotificationIdRepository,
    private val octoPreferences: OctoPreferences,
    private val octoPrintRepository: OctoPrintRepository,
    context: Context
) : ContextWrapper(context) {

    companion object {
        private const val KEY_LAST_PRINT_PREFIX = "last-"
        internal val instance by lazy {
            val context = Injector.get().localizedContext()
            val repository = Injector.get().octorPrintRepository()
            PrintNotificationController(
                context = context,
                notificationFactory = PrintNotificationFactory(context, repository, Injector.get().formatEtaUseCase()),
                octoPreferences = Injector.get().octoPreferences(),
                printNotificationIdRepository = Injector.get().notificationIdRepository(),
                octoPrintRepository = Injector.get().octorPrintRepository()
            )
        }
    }

    private val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val sharedPreferences = getSharedPreferences("print_notification_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationFactory.createNotificationChannels()
        }
    }

    suspend fun createServiceNotification(instance: OctoPrintInstanceInformationV3, statusText: String, doNotify: Boolean = false): Pair<Notification, Int> {
        val notification = getLast(instance.id)?.let { notificationFactory.createStatusNotification(instance.id, it) }
            ?: notificationFactory.createServiceNotification(instance, statusText)
        val id = printNotificationIdRepository.getPrintStatusNotificationId(instance.id)

        if (doNotify) {
            notificationManager.notify(id, notification)
        }

        return notification to id
    }

    suspend fun update(instanceId: String, printState: PrintState?) {
        val last = getLast(instanceId)
        val proceed = when {
            // Notifications disabled? Drop
            octoPreferences.wasPrintNotificationDisabledUntilNextLaunch || !octoPreferences.isPrintNotificationEnabled -> {
                Timber.v("Dropping update, notifications disabled")
                false
            }

            // No last or posting last? Proceed
            last == null -> {
                Timber.v("Proceeding with update, no last found")
                true
            }

            // New live events always proceed
            printState?.source == PrintState.Source.Live -> {
                Timber.v("Proceeding with update, is live")
                true
            }

            // If the last was from remote and this is from remote (implied) and the source time progressed, then we proceed
            last.source == PrintState.Source.CachedRemote && printState != null && printState.sourceTime > last.sourceTime -> {
                Timber.v("Proceeding with update, new and last is remote and time progressed")
                true
            }

            // If the last was live but is outdated, then we proceed
            last.source == PrintState.Source.CachedLive && last.sourceTime.before(liveThreshold) -> {
                Timber.v("Proceeding with update, last is outdated live event")
                true
            }

            // Reposting? Proceed
            last == printState || printState == null -> {
                Timber.v("Proceeding with update, repost")
                true
            }

            // Else: we drop
            else -> {
                Timber.v("Dropping update, default")
                false
            }
        }

        if (proceed) (printState ?: last)?.let {
            setLast(instanceId, it)
            val notificationId = printNotificationIdRepository.getPrintStatusNotificationId(instanceId)
            notificationFactory.createStatusNotification(instanceId, it)?.let {
                Timber.v("Showing print notification: instanceId=$instanceId notificationId=$notificationId")
                notificationManager.notify(notificationId, it)
            } ?: Timber.e(IllegalStateException("Received update event for instance $instanceId but instance was not found"))
        }
    }

    fun notifyCompleted(instanceId: String, printState: PrintState) = notificationFactory.createPrintCompletedNotification(
        instanceId = instanceId,
        printState = printState
    )?.let {
        val notificationId = printNotificationIdRepository.nextPrintEventNotificationId()
        Timber.i("Showing completed notification: instanceId=$instanceId notificationId=$notificationId")
        notificationManager.notify(notificationId, it)
        notifyIdle(instanceId)
    } ?: Timber.e(IllegalStateException("Received completed event for instance $instanceId but instance was not found"))

    suspend fun notifyFilamentRequired(instanceId: String, printState: PrintState) = notificationFactory.createFilamentChangeNotification(
        instanceId = instanceId,
    )?.let {
        val notificationId = printNotificationIdRepository.nextPrintEventNotificationId()
        Timber.i("Showing filament notification: instanceId=$instanceId notificationId=$notificationId")
        update(instanceId, printState)
        notificationManager.notify(notificationId, it)
    } ?: Timber.e(IllegalStateException("Received filament event for instance $instanceId but instance was not found"))

    fun notifyIdle(instanceId: String) {
        val notificationId = printNotificationIdRepository.getPrintStatusNotificationId(instanceId)
        Timber.v("Cancelling print notification: instanceId=$instanceId notificationId=$notificationId")
        clearLast(instanceId)
        notificationManager.cancel(notificationId)
    }

    fun clearLast(instanceId: String) = sharedPreferences.edit { remove("$KEY_LAST_PRINT_PREFIX$instanceId") }

    fun getLast(instanceId: String) = try {
        sharedPreferences.getString("$KEY_LAST_PRINT_PREFIX$instanceId", null)?.let { gson.fromJson(it, PrintState::class.java) }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    private fun setLast(instanceId: String, printState: PrintState) = sharedPreferences.edit {
        putString("$KEY_LAST_PRINT_PREFIX$instanceId", gson.toJson(printState.copy(source = printState.source.asCached)))
    }

    fun cancelUpdateNotifications() {
        octoPrintRepository.getAll().forEach {
            notificationManager.cancel(printNotificationIdRepository.getPrintStatusNotificationId(it.id))
        }
    }

    private val liveThreshold get() = Date(System.currentTimeMillis() - 10_000)

}
