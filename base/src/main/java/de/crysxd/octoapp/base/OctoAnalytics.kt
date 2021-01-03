package de.crysxd.octoapp.base

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import java.util.*

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
        object UserId : UserProperty("user_id")
    }

    sealed class Event(val name: String) {
        object OctoprintConnected : Event("octoprint_connected")

        class PluginDetected(pluginName: String) : Event("zz_has_plugin_${pluginName.toLowerCase(Locale.ENGLISH)}".take(40 /* Limit for event name length */))

        object SupportFromErrorDetails : Event("support_from_error_details")
        object TroubleShootFailureSupportTrigger : Event("troubleshoot_failure_support_trigger")
        object TroubleShootDnsFailure : Event("troubleshoot_failure_dns")
        object TroubleShootHostFailure : Event("troubleshoot_failure_host")
        object TroubleShootPortFailure : Event("troubleshoot_failure_port")
        object TroubleShootHttp1Failure : Event("troubleshoot_failure_http_1")
        object TroubleShootHttp2Failure : Event("troubleshoot_failure_http_2")
        object TroubleShootFromSignIn : Event("troubleshoot_sign_in")

        object QrCodeCompleted : Event("qr_code_login_completed")
        object QrCodeStarted : Event("qr_code_login")
        object SignInFailed : Event("sign_in_failure")
        object SignInInvalidInput : Event("sign_in_invalid_input")
        object SignInNoWifiWarningShown : Event("sign_in_no_wifi")
        object SignInNoWifiWarningTapped : Event("sign_in_no_wifi_tapped")
        object SignInHelpOpened : Event("sign_in_help_opened")
        object SignInSuccess : Event("sign_in_success")
        object Login : Event(FirebaseAnalytics.Event.LOGIN)

        class WidgetInteraction(widgetName: String) : Event("widget_${widgetName}_interaction")
        object AppLanguageChanged : OctoAnalytics.Event("app_language_changed")

        object PrinterAutoConnectFailed : Event("auto_connect_failed")
        object PrintCancelledByApp : Event("print_canceled_by_app")
        object PrintStartedByApp : Event("print_started_by_app")
        object EmergencyStopTriggeredByApp : Event("emergency_stop_triggered_by_app")
        object GcodeSent : Event("gcode_send")

        object PsuCycled : Event("psu_cycle")
        object PsuTurnedOff : Event("psu_turned_off")
        object PsuTurnedOn : Event("psu_turned_on")

        object AppUpdateAvailable : Event("app_update_available")
        object AppUpdatePresented : Event("app_update_presented")

        object ScreenShown : Event(FirebaseAnalytics.Event.SCREEN_VIEW)
        object LoginWorkspaceShown : Event("workspace_shown_login")
        object ConnectWorkspaceShown : Event("workspace_shown_connect")
        object PrePrintWorkspaceShown : Event("workspace_shown_pre_print")
        object PrintWorkspaceShown : Event("workspace_shown_print")
        object TerminalWorkspaceShown : Event("workspace_shown_terminal")

        object BillingNotAvailable : Event("billing_not_available")
        object PurchaseScreenScroll : Event("purchase_screen_scroll")
        object PurchaseScreenOpen : Event("purchase_screen_open")
        object PurchaseOptionsShown : Event("purchase_options_shown")
        object PurchaseIntroShown : Event("purchase_intro_shown")
        object PurchaseScreenClosed : Event("purchase_screen_closed")
        object PurchaseOptionSelected : Event("purchase_option_selected")
        object PurchaseFlowCompleted : Event("purchase_billing_flow_completed")
        object PurchaseFlowCancelled : Event("purchase_billing_flow_cancelled")
        object PurchaseFlowFailed : Event("purchase_billing_flow_failed")
    }
}