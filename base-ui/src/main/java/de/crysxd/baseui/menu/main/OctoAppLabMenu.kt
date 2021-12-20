package de.crysxd.baseui.menu.main

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
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
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.lab_menu___subtitle)

    class NotificationBatterySaver : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().allowNotificationBatterySaver
        override val itemId = ""
        override var groupId = "notification_battery_saver"
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

    class RotationMenuItem : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().allowAppRotation
        override val itemId = "rotate_app"
        override var groupId = ""
        override val canBePinned = false
        override val order = 2
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_screen_rotation_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_title)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().allowAppRotation = enabled
        }
    }

    class SuppressM115Request : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().suppressM115Request
        override val itemId = "suppress_m115"
        override var groupId = ""
        override val canBePinned = false
        override val order = 163
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_block_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_title)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().suppressM115Request = enabled
        }
    }

    class AllowTerminalDuringPrint : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().allowTerminalDuringPrint
        override val itemId = "allow_terminal_during_print"
        override var groupId = ""
        override val canBePinned = false
        override val order = 164
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_code_off_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_terminal_during_print_title)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_terminal_during_print_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().allowTerminalDuringPrint = enabled
        }
    }

    class SuppressRemoteNotificationInitialization : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().suppressRemoteMessageInitialization
        override val itemId = "suppress_remote"
        override var groupId = ""
        override val canBePinned = false
        override val order = 165
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_notifications_off_24

        override fun getTitle(context: Context) = context.getString(R.string.lab_menu___suppress_remote_notification_init)
        override fun getDescription(context: Context) = context.getString(R.string.lab_menu___suppress_remote_notification_init_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().suppressRemoteMessageInitialization = enabled
        }
    }
}