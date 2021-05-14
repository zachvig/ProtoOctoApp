package de.crysxd.octoapp.base

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import java.util.*

@Suppress("EXPERIMENTAL_API_USAGE")
class OctoPreferences(private val sharedPreferences: SharedPreferences) {

    companion object {
        private const val KEY_PRINT_NOTIFICATION_ENABLED = "print_notification_enabled"
        private const val KEY_MANUAL_DARK_MODE = "manual_dark_mode_enabled"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_ALLOW_APP_ROTATION = "allow_app_rotation"
        private const val KEY_ALLOW_NOTIFICATION_BATTERY_SAVER = "allow_notification_battery_saver"
        private const val KEY_HIDE_THUMBNAIL_HINT_UNTIL = "hide_thumbnail_hin_until"
        private const val KEY_ACTIVE_INSTANCE_WEB_URL = "active_instance_web_url"
        private const val KEY_AUTO_CONNECT_PRINTER = "auto_connect_printer"
        private const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
        private const val KEY_ANALYTICS = "analytics_enabled"
        private const val KEY_PRINT_NOTIFICATION_WAS_DISCONNECTED = "print_notification_was_disconnected"
        private const val KEY_PRINT_NOTIFICATION_WAS_PAUSED = "print_notification_was_paused"
        private const val KEY_EXPERIMENTAL_WEBCAM = "experimental_webcam"
        private const val KEY_AUTO_LIGHTS = "auto_lights"
        private const val KEY_AUTO_LIGHTS_FOR_WIDGET_REFRESH = "auto_lights_for_widget_refresh"
    }

    private val updatedChannel = ConflatedBroadcastChannel(Unit)
    val updatedFlow get() = updatedChannel.asFlow()

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sharedPreferences.edit(action = block)
        updatedChannel.offer(Unit)
    }

    var wasPrintNotificationDisconnected: Boolean
        get() = sharedPreferences.getBoolean(KEY_PRINT_NOTIFICATION_WAS_DISCONNECTED, false)
        set(value) {
            edit { putBoolean(KEY_PRINT_NOTIFICATION_WAS_DISCONNECTED, value) }
        }

    var wasPrintNotificationPaused: Boolean
        get() = sharedPreferences.getBoolean(KEY_PRINT_NOTIFICATION_WAS_PAUSED, false)
        set(value) {
            edit { putBoolean(KEY_PRINT_NOTIFICATION_WAS_PAUSED, value) }
        }

    var isAnalyticsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_ANALYTICS, true)
        set(value) {
            edit { putBoolean(KEY_ANALYTICS, value) }
            Firebase.analytics.setAnalyticsCollectionEnabled(value)
        }

    var isCrashReportingEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_CRASH_REPORTING, true)
        set(value) {
            edit { putBoolean(KEY_CRASH_REPORTING, value) }
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(value)
        }

    var activeInstanceWebUrl: String?
        get() = sharedPreferences.getString(KEY_ACTIVE_INSTANCE_WEB_URL, null)
        set(value) {
            edit { putString(KEY_ACTIVE_INSTANCE_WEB_URL, value) }
        }

    var isKeepScreenOnDuringPrint
        get() = sharedPreferences.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) {
            edit { putBoolean(KEY_KEEP_SCREEN_ON, value) }
        }

    var isAutoConnectPrinter
        get() = sharedPreferences.getBoolean(KEY_AUTO_CONNECT_PRINTER, true)
        set(value) {
            edit { putBoolean(KEY_AUTO_CONNECT_PRINTER, value) }
        }

    var isPrintNotificationEnabled
        get() = sharedPreferences.getBoolean(KEY_PRINT_NOTIFICATION_ENABLED, true)
        set(value) {
            edit { putBoolean(KEY_PRINT_NOTIFICATION_ENABLED, value) }
        }

    var isManualDarkModeEnabled
        get() = sharedPreferences.getBoolean(KEY_MANUAL_DARK_MODE, false)
        set(value) {
            edit { putBoolean(KEY_MANUAL_DARK_MODE, value) }
            Injector.get().applyLegacyDarkModeUseCase().executeBlocking(Unit)
        }

    var appLanguage
        get() = sharedPreferences.getString(KEY_APP_LANGUAGE, null)
        set(value) {
            edit { putString(KEY_APP_LANGUAGE, value) }
        }

    var allowAppRotation
        get() = sharedPreferences.getBoolean(KEY_ALLOW_APP_ROTATION, false)
        set(value) {
            edit { putBoolean(KEY_ALLOW_APP_ROTATION, value) }
        }

    var allowNotificationBatterySaver
        get() = sharedPreferences.getBoolean(KEY_ALLOW_NOTIFICATION_BATTERY_SAVER, Firebase.remoteConfig.getBoolean("notification_battery_saver"))
        set(value) {
            edit { putBoolean(KEY_ALLOW_NOTIFICATION_BATTERY_SAVER, value) }
        }

    var hideThumbnailHintUntil
        get() = Date(sharedPreferences.getLong(KEY_HIDE_THUMBNAIL_HINT_UNTIL, 0))
        set(value) {
            edit { putLong(KEY_HIDE_THUMBNAIL_HINT_UNTIL, value.time) }
        }

    var experimentalWebcam
        get() = sharedPreferences.getBoolean(KEY_EXPERIMENTAL_WEBCAM, Firebase.remoteConfig.getBoolean("experimental_webcam"))
        set(value) {
            edit { putBoolean(KEY_EXPERIMENTAL_WEBCAM, value) }
        }

    var automaticLights
        get() = sharedPreferences.getStringSet(KEY_AUTO_LIGHTS, emptySet()) ?: emptySet()
        set(value) {
            edit { putStringSet(KEY_AUTO_LIGHTS, value) }
        }

    var automaticLightsForWidgetRefresh
        get() = sharedPreferences.getBoolean(KEY_AUTO_LIGHTS_FOR_WIDGET_REFRESH, false)
        set(value) {
            edit { putBoolean(KEY_AUTO_LIGHTS_FOR_WIDGET_REFRESH, value) }
        }
}