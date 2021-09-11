package de.crysxd.octoapp.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import timber.log.Timber
import java.util.Date

class PrintNotificationController(
    private val notificationFactory: PrintNotificationFactory,
    private val octoPrintRepository: OctoPrintRepository,
    context: Context
) : ContextWrapper(context) {

    companion object {
        private const val KEY_LAST_EVENT_NOTIFICATION_ID = "last-event-notification-id"
        private const val KEY_LAST_PRINT_PREFIX = "last-"
        private const val KEY_PRINT_NOTIFICATION_PREFIX = "notification-id-"
        private val PRINT_STATUS_NOTIFICATION_ID_RANGE = 15_000..15_099
        private val PRINT_EVENT_NOTIFICATION_ID_RANGE = 15_100..15_199

        internal val instance by lazy {
            val context = Injector.get().localizedContext()
            val repository = Injector.get().octorPrintRepository()
            PrintNotificationController(
                context = context,
                notificationFactory = PrintNotificationFactory(context, repository, Injector.get().formatEtaUseCase()),
                octoPrintRepository = repository
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

    suspend fun createServiceNotification(instanceId: String, statusText: String, doNotify: Boolean = false): Pair<Notification, Int> {
        val notification = getLast(instanceId)?.let { notificationFactory.createStatusNotification(instanceId, it) }
            ?: notificationFactory.createServiceNotification(statusText)
        val id = getNotificationId(instanceId)

        if (doNotify) {
            notificationManager.notify(id, notification)
        }

        return notification to id
    }

    suspend fun update(instanceId: String, print: Print?) {
        val last = getLast(instanceId)
        val proceed = when {
            // No last or posting last? Proceed
            last == null -> true

            // New live events always proceed
            print?.source == Print.Source.Live -> true

            // If the last was from remote and this is from remote (implied) and the source time progressed, then we proceed
            last.source == Print.Source.CachedRemote && print != null && print.sourceTime > last.sourceTime -> true

            // If the last was live but is outdated, then we proceed
            last.source == Print.Source.CachedLive && last.sourceTime.before(liveThreshold) -> true

            // Reposting? Proceed
            last == print || print == null -> true

            // Else: we drop
            else -> false
        }

        if (proceed) (print ?: last)?.let {
            setLast(instanceId, it)
            val notificationId = getNotificationId(instanceId)
            notificationFactory.createStatusNotification(instanceId, it)?.let {
                Timber.i("Showing print notification: instanceId=$instanceId notificationId=$notificationId")
                notificationManager.notify(notificationId, it)
            } ?: Timber.e(IllegalStateException("Received update event for instance $instanceId but instance was not found"))
        }
    }

    fun notifyCompleted(instanceId: String, print: Print) = notificationFactory.createPrintCompletedNotification(
        instanceId = instanceId,
        print = print
    )?.let {
        val notificationId = nextEventNotificationId()
        Timber.i("Showing completed notification: instanceId=$instanceId notificationId=$notificationId")
        notificationManager.notify(notificationId, it)
    } ?: Timber.e(IllegalStateException("Received completed event for instance $instanceId but instance was not found"))

    fun notifyFilamentRequired(instanceId: String, print: Print) = notificationFactory.createFilamentChangeNotification(
        instanceId = instanceId,
    )?.let {
        val notificationId = nextEventNotificationId()
        Timber.i("Showing filament notification: instanceId=$instanceId notificationId=$notificationId")
        notificationManager.notify(nextEventNotificationId(), it)
    } ?: Timber.e(IllegalStateException("Received filament event for instance $instanceId but instance was not found"))

    private fun getLast(instanceId: String) = sharedPreferences.getString("$KEY_LAST_PRINT_PREFIX$instanceId", null)?.let { gson.fromJson(it, Print::class.java) }

    private fun setLast(instanceId: String, print: Print) = sharedPreferences.edit {
        putString("$KEY_LAST_PRINT_PREFIX$instanceId", gson.toJson(print.copy(source = print.source.asCached)))
    }

    private val liveThreshold get() = Date(System.currentTimeMillis() - 30_000)

    private fun getNotificationId(instanceId: String): Int {
        val key = "$KEY_PRINT_NOTIFICATION_PREFIX$instanceId"

        // Clean up
        val notificationIdKeys = sharedPreferences.all.keys.filter { it.startsWith(KEY_PRINT_NOTIFICATION_PREFIX) }
        val toDelete = notificationIdKeys.filter {
            val id = it.removePrefix(KEY_PRINT_NOTIFICATION_PREFIX)
            octoPrintRepository.getAll().none { it.id == id }
        }
        sharedPreferences.edit {
            toDelete.forEach { remove(it) }
        }

        // Find allocated or create new
        return sharedPreferences.getInt(key, -1).takeIf { it >= 0 } ?: let {
            // Nothing allocated yet...allocate a new notification id for this instance
            // Find min free
            val notificationIds = sharedPreferences.all.filter { it.key.startsWith(KEY_PRINT_NOTIFICATION_PREFIX) }.mapNotNull { it.value as? Int }
            val notificationId = PRINT_STATUS_NOTIFICATION_ID_RANGE.firstOrNull {
                !notificationIds.contains(it)
            } ?: notificationIds.maxOf { it } + 1

            // Store and return
            sharedPreferences.edit { putInt(key, notificationId) }
            notificationId
        }
    }

    private fun nextEventNotificationId(): Int {
        val last = sharedPreferences.getInt(KEY_LAST_EVENT_NOTIFICATION_ID, 0)
        val next = PRINT_EVENT_NOTIFICATION_ID_RANGE.nextAfter(last)

        sharedPreferences.edit {
            putInt(KEY_LAST_EVENT_NOTIFICATION_ID, next)
        }
        return next
    }

    private fun IntRange.nextAfter(i: Int) = ((i + step) % PRINT_EVENT_NOTIFICATION_ID_RANGE.last).coerceAtLeast(PRINT_EVENT_NOTIFICATION_ID_RANGE.first)
}
