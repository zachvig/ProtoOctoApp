package de.crysxd.octoapp.base

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

object OctoAnalytics {

    fun logEvent(event: Event, params: Bundle = Bundle.EMPTY) {
        Timber.tag("Analytics/Event").i("${event.name} / $params")
        Firebase.analytics.logEvent(event.name, params)
    }

    fun setUserProperty(property: UserProperty, value: String?) {
        Timber.tag("Analytics/Property").i("${property.name}=$value")
        Firebase.analytics.setUserProperty(property.name, value)
    }

    sealed class UserProperty(val name: String) {
        object OctoPrintVersion : UserProperty("octoprint_server_version")
        object UserIsAdmin : UserProperty("octoprint_server_admin")
        object WebCamAvailable : UserProperty("webcam_available")
        object AppLanguage : UserProperty("app_language")
        object PremiumUser : UserProperty("premium_user")
        object PremiumSubUser : UserProperty("premium_sub_user")
        object BillingStatus : UserProperty("billing_status")
        object UserId : UserProperty("user_id")
        object OctoEverywhereUser : UserProperty("octoeverywhere_user")
        object RemoteAccess : UserProperty("remote_access_configured")
    }

    sealed class Event(val name: String) {
        object OctoprintConnected : Event("octoprint_connected")

        class PluginDetected(pluginName: String) : Event("zz_has_plugin_${pluginName.lowercase()}".take(40 /* Limit for event name length */))
        object SupportFromErrorDetails : Event("support_from_error_details")
        object BackupDnsResolveSuccess : Event("backup_dns_resolve_success")
        object MDnsResolveSuccess : Event("mdns_resolve_success")
        object UpnpDnsResolveSuccess : Event("upnp_dns_resolve_success")

        object QrCodeCompleted : Event("sign_in_qr_code_login_completed")
        object QrCodeStarted : Event("sign_in_qr_code_login")
        object ManualApiKeyUsed : Event("sign_in_manual_api_key_used")
        object SignInHelpOpened : Event("sign_in_help_opened")
        object SignInSuccess : Event("sign_in_success")
        object Login : Event(FirebaseAnalytics.Event.LOGIN)
        object UpnpServiceSelected : Event("sign_in_upnp_service_selected")
        object DnsServiceSelected : Event("sign_in_dnssd_service_selected")
        object ManualUrlSelected : Event("sign_in_manual_url_selected")

        class WidgetInteraction(widgetName: String) : Event("widget_${widgetName}_interaction")
        object AppLanguageChanged : OctoAnalytics.Event("app_language_changed")

        object PrinterAutoConnectFailed : Event("auto_connect_failed")
        object PrintCancelledByApp : Event("print_canceled_by_app")
        object PrintStartedByApp : Event("print_started_by_app")
        object EmergencyStopTriggeredByApp : Event("emergency_stop_triggered_by_app")
        object GcodeSent : Event("gcode_send")

        object PsuCycled : Event("psu_cycle")
        object PsuTurnedOff : Event("psu_turned_off")
        object PsuToggled : Event("psu_toggle")
        object PsuTurnedOn : Event("psu_turned_on")

        object AppUpdateAvailable : Event("app_update_available")
        object AppUpdatePresented : Event("app_update_presented")

        object ScreenShown : Event(FirebaseAnalytics.Event.SCREEN_VIEW)
        object LoginWorkspaceShown : Event("workspace_shown_login")
        object ConnectWorkspaceShown : Event("workspace_shown_connect")
        object PrePrintWorkspaceShown : Event("workspace_shown_pre_print")
        object PrintWorkspaceShown : Event("workspace_shown_print")
        object TerminalWorkspaceShown : Event("workspace_shown_terminal")

        object PurchaseScreenScroll : Event("purchase_screen_scroll")
        object PurchaseScreenOpen : Event("purchase_screen_open")
        object PurchaseMissingSku : Event("purchase_missing_sku")
        object PurchaseOptionsShown : Event("purchase_options_shown")
        object PurchaseIntroShown : Event("purchase_intro_shown")
        object PurchaseScreenClosed : Event("purchase_screen_closed")
        object PurchaseOptionSelected : Event("purchase_option_selected")
        object PurchaseFlowCompleted : Event("purchase_billing_flow_completed")
        object PurchaseFlowCancelled : Event("purchase_billing_flow_cancelled")
        object PurchaseFlowFailed : Event("purchase_billing_flow_failed")
        object DisabledFeatureHidden : Event("disabled_feature_hidden")

        object RemoteConfigScreenOpened : Event("remote_config_screen_opened")
        object RemoteConfigManuallySet : Event("remote_config_manually_set")
        object RemoteConfigManuallySetFailed : Event("remote_config_manually_set_failed")
        object OctoEverywhereConnectStarted: Event("octoeverywhere_connect_started")
        object OctoEverywhereConnected: Event("octoeverywhere_connected")
        object OctoEverywhereConnectFailed: Event("octoeverywhere_connect_failed")
        object OctoEverywherePluginMissing: Event("octoeverywhere_plugin_missing")
    }
}