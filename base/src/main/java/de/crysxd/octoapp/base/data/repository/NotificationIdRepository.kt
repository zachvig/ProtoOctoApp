package de.crysxd.octoapp.base.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import de.crysxd.octoapp.base.ext.nextAfter

class NotificationIdRepository(
    private val sharedPreferences: SharedPreferences,
    private val octoPrintRepository: OctoPrintRepository,
) {
    companion object {
        private const val PRINT_STATUS_FALLBACK_NOTIFICATION_ID = 14_999
        private val PRINT_STATUS_NOTIFICATION_ID_RANGE = 15_000..15_099
        private val PRINT_EVENT_NOTIFICATION_ID_RANGE = 15_100..15_199
        private val UPDATE_NOTIFICATION_ID_RANGE = 15_200..15_299
        private const val REQUEST_ACCESS_COMPLETED_NOTIFICATION_ID = 3432


        private const val KEY_PRINT_NOTIFICATION_PREFIX = "notification-id-"
        private const val KEY_LAST_PRINT_EVENT_NOTIFICATION_ID = "last-print-event-notification-id"
        private const val KEY_LAST_UPDATE_NOTIFICATION_ID = "last-update-notification-id"
    }

    val requestAccessCompletedNotificationId = REQUEST_ACCESS_COMPLETED_NOTIFICATION_ID


    fun getPrintStatusNotificationId(instanceId: String?): Int {
        instanceId ?: return PRINT_STATUS_FALLBACK_NOTIFICATION_ID
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

    fun nextPrintEventNotificationId(): Int {
        val last = sharedPreferences.getInt(KEY_LAST_PRINT_EVENT_NOTIFICATION_ID, 0)
        val next = PRINT_EVENT_NOTIFICATION_ID_RANGE.nextAfter(last)

        sharedPreferences.edit {
            putInt(KEY_LAST_PRINT_EVENT_NOTIFICATION_ID, next)
        }
        return next
    }

    fun nextUpdateNotificationId(): Int {
        val last = sharedPreferences.getInt(KEY_LAST_UPDATE_NOTIFICATION_ID, 0)
        val next = UPDATE_NOTIFICATION_ID_RANGE.nextAfter(last)

        sharedPreferences.edit {
            putInt(KEY_LAST_UPDATE_NOTIFICATION_ID, next)
        }
        return next
    }
}