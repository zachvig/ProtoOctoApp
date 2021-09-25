package de.crysxd.octoapp.base

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date

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
        private const val KEY_ACTIVE_INSTANCE_ID = "active_instance_id"
        private const val KEY_AUTO_CONNECT_PRINTER = "auto_connect_printer"
        private const val KEY_CRASH_REPORTING = "crash_reporting_enabled"
        private const val KEY_ANALYTICS = "analytics_enabled"
        private const val KEY_PRINT_NOTIFICATION_WAS_DISCONNECTED = "print_notification_was_disconnected"
        private const val KEY_PRINT_NOTIFICATION_WAS_DISABLED_UNTIL_NEXT_LAUNCH = "print_notification_was_disabled_until_next_launch"
        private const val KEY_AUTO_LIGHTS = "auto_lights"
        private const val KEY_CONFIRM_POWER_OFF_DEVICES = "confirm_power_off_devices"
        private const val KEY_AUTO_LIGHTS_FOR_WIDGET_REFRESH = "auto_lights_for_widget_refresh"
        private const val KEY_SHOW_WEBCAM_RESOLUTION = "show_webcam_resolution"
        private const val KEY_WEBCAM_ASPECT_RATIO_SOURCE = "webcam_aspect_ratio_source"
        private const val KEY_SUPPRESS_M115 = "suppress_m115_request"
        private const val KEY_COMPANION_ANNOUNCEMENT_HIDDEN_AT = "companion_announcemenyt_hidden_at"
        private const val KEY_OCTOEVERYWHERE_ANNOUNCEMENT_HIDDEN_AT = "octoeverywhere_announcemenyt_hidden_at"
        private const val KEY_TUTORIALS_SEEN_AT = "tutorials_seen_at"

        const val VALUE_WEBCAM_ASPECT_RATIO_SOURCE_OCTOPRINT = "octprint"
        const val VALUE_WEBCAM_ASPECT_RATIO_SOURCE_IMAGE = "native_image"
    }

    private val updatedChannel = MutableStateFlow(0)
    val updatedFlow get() = updatedChannel.asStateFlow().map { }

    init {
        // Delete legacy
        sharedPreferences.edit {
            remove("print_notification_was_paused")
        }
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sharedPreferences.edit(action = block)
        updatedChannel.value++
    }

    var wasPrintNotificationDisconnected: Boolean
        get() = sharedPreferences.getBoolean(KEY_PRINT_NOTIFICATION_WAS_DISCONNECTED, false)
        set(value) {
            edit { putBoolean(KEY_PRINT_NOTIFICATION_WAS_DISCONNECTED, value) }
        }

    var wasPrintNotificationDisabledUntilNextLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_PRINT_NOTIFICATION_WAS_DISABLED_UNTIL_NEXT_LAUNCH, false)
        set(value) {
            edit { putBoolean(KEY_PRINT_NOTIFICATION_WAS_DISABLED_UNTIL_NEXT_LAUNCH, value) }
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

    @Deprecated("Use activeInstanceId instead")
    var activeInstanceWebUrl: String?
        get() = sharedPreferences.getString(KEY_ACTIVE_INSTANCE_WEB_URL, null)
        set(value) {
            edit { putString(KEY_ACTIVE_INSTANCE_WEB_URL, value) }
        }

    var activeInstanceId: String?
        get() = sharedPreferences.getString(KEY_ACTIVE_INSTANCE_ID, null)
        set(value) {
            edit { putString(KEY_ACTIVE_INSTANCE_ID, value) }
        }

    var companionAnnouncementHiddenAt: Date?
        get() = sharedPreferences.getLong(KEY_COMPANION_ANNOUNCEMENT_HIDDEN_AT, 0).takeIf { it > 0 }?.let { Date(it) }
        set(value) {
            edit { putLong(KEY_COMPANION_ANNOUNCEMENT_HIDDEN_AT, value?.time ?: 0) }
        }

    var octoEverywhereAnnouncementHiddenAt: Date?
        get() = sharedPreferences.getLong(KEY_OCTOEVERYWHERE_ANNOUNCEMENT_HIDDEN_AT, 0).takeIf { it > 0 }?.let { Date(it) }
        set(value) {
            edit { putLong(KEY_OCTOEVERYWHERE_ANNOUNCEMENT_HIDDEN_AT, value?.time ?: 0) }
        }

    var tutorialsSeenAt: Date?
        get() = sharedPreferences.getLong(KEY_TUTORIALS_SEEN_AT, 0).takeIf { it > 0 }?.let { Date(it) }
        set(value) {
            edit { putLong(KEY_TUTORIALS_SEEN_AT, value?.time ?: 0) }
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

    var isLivePrintNotificationsEnabled
        get() = sharedPreferences.getBoolean(KEY_PRINT_NOTIFICATION_ENABLED, true)
        set(value) {
            edit { putBoolean(KEY_PRINT_NOTIFICATION_ENABLED, value) }
        }

    var isManualDarkModeEnabled
        get() = sharedPreferences.getBoolean(KEY_MANUAL_DARK_MODE, false)
        set(value) {
            edit { putBoolean(KEY_MANUAL_DARK_MODE, value) }
            BaseInjector.get().applyLegacyDarkModeUseCase().executeBlocking(Unit)
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

    var automaticLights
        get() = sharedPreferences.getStringSet(KEY_AUTO_LIGHTS, emptySet()) ?: emptySet()
        set(value) {
            edit { putStringSet(KEY_AUTO_LIGHTS, value) }
        }

    var confirmPowerOffDevices
        get() = sharedPreferences.getStringSet(KEY_CONFIRM_POWER_OFF_DEVICES, emptySet()) ?: emptySet()
        set(value) {
            edit { putStringSet(KEY_CONFIRM_POWER_OFF_DEVICES, value) }
        }

    var automaticLightsForWidgetRefresh
        get() = sharedPreferences.getBoolean(KEY_AUTO_LIGHTS_FOR_WIDGET_REFRESH, false)
        set(value) {
            edit { putBoolean(KEY_AUTO_LIGHTS_FOR_WIDGET_REFRESH, value) }
        }

    var isShowWebcamResolution
        get() = sharedPreferences.getBoolean(KEY_SHOW_WEBCAM_RESOLUTION, true)
        set(value) {
            edit { putBoolean(KEY_SHOW_WEBCAM_RESOLUTION, value) }
        }

    var webcamAspectRatioSource
        get() = sharedPreferences.getString(KEY_WEBCAM_ASPECT_RATIO_SOURCE, VALUE_WEBCAM_ASPECT_RATIO_SOURCE_OCTOPRINT) ?: VALUE_WEBCAM_ASPECT_RATIO_SOURCE_OCTOPRINT
        set(value) {
            edit { putString(KEY_WEBCAM_ASPECT_RATIO_SOURCE, value) }
        }

    var suppressM115Request
        get() = sharedPreferences.getBoolean(KEY_SUPPRESS_M115, false)
        set(value) {
            edit { putBoolean(KEY_SUPPRESS_M115, value) }
        }
}