package de.crysxd.baseui.menu.main

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.parcelize.Parcelize

@Parcelize
class OctoAppLabMenu : Menu {
    override suspend fun getMenuItem() = listOf(
        RotationMenuItem(),
        NotificationBatterySaver(),
        SuppressM115Request(),
        AllowTerminalDuringPrint(),
        SuppressRemoteNotificationInitialization(),
        DebugNetworkLogging(),
        UseCustomDns(),
        RecordWebcam(),
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.lab_menu___subtitle)

    private class NotificationBatterySaver : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().allowNotificationBatterySaver
        override val itemId = "notification_battery_saver"
        override var groupId = "notification"
        override val canBePinned = false
        override val order = 1
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_battery_charging_full_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_battery_saver_title)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_battery_saver_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().allowNotificationBatterySaver = enabled
        }
    }

    private class SuppressRemoteNotificationInitialization : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().suppressRemoteMessageInitialization
        override val itemId = "suppress_remote"
        override var groupId = "notification"
        override val canBePinned = false
        override val order = 2
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_notifications_off_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___suppress_remote_notification_init)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___suppress_remote_notification_init_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().suppressRemoteMessageInitialization = enabled
        }
    }

    private class RotationMenuItem : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().allowAppRotation
        override val itemId = "rotate_app"
        override var groupId = "misc"
        override val canBePinned = false
        override val order = 100
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_screen_rotation_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_title)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().allowAppRotation = enabled
        }
    }

    private class SuppressM115Request : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().suppressM115Request
        override val itemId = "suppress_m115"
        override var groupId = "misc"
        override val canBePinned = false
        override val order = 101
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_block_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_title)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().suppressM115Request = enabled
        }
    }

    private class AllowTerminalDuringPrint : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().allowTerminalDuringPrint
        override val itemId = "allow_terminal_during_print"
        override var groupId = "misc"
        override val canBePinned = false
        override val order = 101
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_code_off_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_terminal_during_print_title)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_terminal_during_print_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().allowTerminalDuringPrint = enabled
        }
    }

    private class DebugNetworkLogging : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().debugNetworkLogging
        override val itemId = "debug_network_logging"
        override var groupId = "network"
        override val canBePinned = false
        override val order = 50
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_bug_report_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___debug_network_logging)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___debug_network_logging_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().debugNetworkLogging = enabled
        }
    }

    private class UseCustomDns : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().useCustomDns
        override val itemId = "user_custom_dns"
        override var groupId = "network"
        override val canBePinned = false
        override val order = 51
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_dns_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___use_custom_dns)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___use_custom_dns_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().useCustomDns = enabled
        }
    }

    private class RecordWebcam : MenuItem {
        override val itemId = "record_webcam"
        override var groupId = "webcam"
        override val canBePinned = false
        override val order = 52
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_share_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___record_webcam_traffic_for_debug)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___record_webcam_traffic_for_debug_description)
        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().octoPreferences().recordWebcamForDebug = true
        }
    }
}