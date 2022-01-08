package de.crysxd.octoapp.base

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import de.crysxd.octoapp.base.data.models.FileManagerSettings
import de.crysxd.octoapp.base.data.models.GcodePreviewSettings
import de.crysxd.octoapp.base.data.models.ProgressWidgetSettings
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date

class OctoPreferences(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
) {

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
        private const val KEY_AUTO_CONNECT_PRINTER_INFO_SHOWN = "auto_connect_printer_info_shown"
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
        private const val KEY_REMOTE_ACCESS_ANNOUNCEMENT_HIDDEN_AT = "octoeverywhere_announcemenyt_hidden_at"
        private const val KEY_TUTORIALS_SEEN_AT = "tutorials_seen_at"
        private const val KEY_ALLOW_TERMINAL_DURING_PRINT = "allow_terminal_during_print"
        private const val KEY_SUPPRESS_REMOTE_NOTIFICATIONS_INIT = "suppress_remote_notification_init"
        private const val KEY_DEBUG_NETWORK_LOGGING = "debug_network_logging"
        private const val KEY_USE_CUSTOM_DNS = "use_custom_dns"
        private const val KEY_USE_LEGACY_WEBCAM = "use_legacy_webcam"
        private const val KEY_RECORD_WEBCAM_FOR_DEBUG = "record_webcam_for_debug"
        private const val KEY_GCODE_PREVIEW = "gcode_preview"
        private const val KEY_FILE_MANAGER = "file_manager"
        private const val KEY_PROGRESS_WIDGET = "progress_widget"
        private const val KEY_ASK_FOR_TIMELAPSE_BEFORE_PRINTING = "ask_for_timelapse_before_printing"

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

    var remoteAccessAnnouncementHiddenAt: Date?
        get() = sharedPreferences.getLong(KEY_REMOTE_ACCESS_ANNOUNCEMENT_HIDDEN_AT, 0).takeIf { it > 0 }?.let { Date(it) }
        set(value) {
            edit { putLong(KEY_REMOTE_ACCESS_ANNOUNCEMENT_HIDDEN_AT, value?.time ?: 0) }
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
        get() = sharedPreferences.getBoolean(KEY_AUTO_CONNECT_PRINTER, false)
        set(value) {
            edit { putBoolean(KEY_AUTO_CONNECT_PRINTER, value) }
        }

    var wasAutoConnectPrinterInfoShown
        get() = sharedPreferences.getBoolean(KEY_AUTO_CONNECT_PRINTER_INFO_SHOWN, false)
        set(value) {
            edit { putBoolean(KEY_AUTO_CONNECT_PRINTER_INFO_SHOWN, value) }
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
        get() = sharedPreferences.getString(KEY_WEBCAM_ASPECT_RATIO_SOURCE, VALUE_WEBCAM_ASPECT_RATIO_SOURCE_IMAGE) ?: VALUE_WEBCAM_ASPECT_RATIO_SOURCE_IMAGE
        set(value) {
            edit { putString(KEY_WEBCAM_ASPECT_RATIO_SOURCE, value) }
        }

    var suppressM115Request
        get() = sharedPreferences.getBoolean(KEY_SUPPRESS_M115, false)
        set(value) {
            edit { putBoolean(KEY_SUPPRESS_M115, value) }
        }

    var allowTerminalDuringPrint
        get() = sharedPreferences.getBoolean(KEY_ALLOW_TERMINAL_DURING_PRINT, false)
        set(value) {
            edit { putBoolean(KEY_ALLOW_TERMINAL_DURING_PRINT, value) }
        }

    var suppressRemoteMessageInitialization
        get() = sharedPreferences.getBoolean(KEY_SUPPRESS_REMOTE_NOTIFICATIONS_INIT, false)
        set(value) {
            edit { putBoolean(KEY_SUPPRESS_REMOTE_NOTIFICATIONS_INIT, value) }
        }

    var debugNetworkLogging
        get() = sharedPreferences.getBoolean(KEY_DEBUG_NETWORK_LOGGING, false)
        set(value) {
            edit { putBoolean(KEY_DEBUG_NETWORK_LOGGING, value) }
        }

    var useCustomDns
        get() = sharedPreferences.getBoolean(KEY_USE_CUSTOM_DNS, true)
        set(value) {
            edit { putBoolean(KEY_USE_CUSTOM_DNS, value) }
        }

    var useLegacyWebcam
        get() = sharedPreferences.getBoolean(KEY_USE_LEGACY_WEBCAM, false)
        set(value) {
            edit { putBoolean(KEY_USE_LEGACY_WEBCAM, value) }
        }

    var recordWebcamForDebug
        get() = sharedPreferences.getBoolean(KEY_RECORD_WEBCAM_FOR_DEBUG, false)
        set(value) {
            edit { putBoolean(KEY_RECORD_WEBCAM_FOR_DEBUG, value) }
        }

    var askForTimelapseBeforePrinting
        get() = sharedPreferences.getBoolean(KEY_ASK_FOR_TIMELAPSE_BEFORE_PRINTING, false)
        set(value) {
            edit { putBoolean(KEY_ASK_FOR_TIMELAPSE_BEFORE_PRINTING, value) }
        }

    var gcodePreviewSettings: GcodePreviewSettings
        get() = sharedPreferences.getString(KEY_GCODE_PREVIEW, null)?.let {
            gson.fromJson(it, GcodePreviewSettings::class.java)
        } ?: GcodePreviewSettings()
        set(value) {
            edit { putString(KEY_GCODE_PREVIEW, gson.toJson(value)) }
        }

    var fileManagerSettings: FileManagerSettings
        get() = sharedPreferences.getString(KEY_FILE_MANAGER, null)?.let {
            gson.fromJson(it, FileManagerSettings::class.java)
        } ?: FileManagerSettings()
        set(value) {
            edit { putString(KEY_FILE_MANAGER, gson.toJson(value)) }
        }

    var progressWidgetSettings: ProgressWidgetSettings
        get() = sharedPreferences.getString(KEY_PROGRESS_WIDGET, null)?.let {
            gson.fromJson(it, ProgressWidgetSettings::class.java)
        } ?: ProgressWidgetSettings()
        set(value) {
            edit { putString(KEY_PROGRESS_WIDGET, gson.toJson(value)) }
        }
}