package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuHost
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.ToggleMenuItem
import kotlinx.parcelize.Parcelize

@Parcelize
class OctoAppLabMenu : Menu {
    override suspend fun getMenuItem() = listOf(
        RotationMenuItem(),
        NotificationBatterySaver(),
        ExperimentalWebcam(),
        SuppressM115Request(),
        AllowTerminalDuringPrint(),
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.lab_menu___subtitle)

    class NotificationBatterySaver : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().allowNotificationBatterySaver
        override val itemId = ""
        override var groupId = "notification_battery_saver"
        override val canBePinned = false
        override val order = 1
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_battery_charging_full_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_battery_saver_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_battery_saver_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            Injector.get().octoPreferences().allowNotificationBatterySaver = enabled
        }
    }

    class RotationMenuItem : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().allowAppRotation
        override val itemId = "rotate_app"
        override var groupId = ""
        override val canBePinned = false
        override val order = 2
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_screen_rotation_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            Injector.get().octoPreferences().allowAppRotation = enabled
        }
    }

    class ExperimentalWebcam : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().experimentalWebcam
        override val itemId = "experimental_webcam"
        override var groupId = ""
        override val canBePinned = false
        override val order = 162
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_videocam_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___experimental_webcam_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___experimental_webcam_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            Injector.get().octoPreferences().experimentalWebcam = enabled
        }
    }

    class SuppressM115Request : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().suppressM115Request
        override val itemId = "suppress_m115"
        override var groupId = ""
        override val canBePinned = false
        override val order = 163
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_block_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            Injector.get().octoPreferences().suppressM115Request = enabled
        }
    }

    class AllowTerminalDuringPrint : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().allowTerminalDuringPrint
        override val itemId = "allow_terminal_during_print"
        override var groupId = ""
        override val canBePinned = false
        override val order = 164
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_code_off_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_terminal_during_print_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_terminal_during_print_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            Injector.get().octoPreferences().allowTerminalDuringPrint = enabled
        }
    }
}