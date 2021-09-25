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
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.lab_menu___subtitle)

    class NotificationBatterySaver : ToggleMenuItem() {
        override val isEnabled get() = BaseInjector.get().octoPreferences().allowNotificationBatterySaver
        override val itemId = ""
        override var groupId = "notification_battery_saver"
        override val canBePinned = false
        override val order = 1
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_battery_charging_full_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_battery_saver_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_battery_saver_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().allowNotificationBatterySaver = enabled
        }
    }

    class RotationMenuItem : ToggleMenuItem() {
        override val isEnabled get() = BaseInjector.get().octoPreferences().allowAppRotation
        override val itemId = "rotate_app"
        override var groupId = ""
        override val canBePinned = false
        override val order = 2
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_screen_rotation_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().allowAppRotation = enabled
        }
    }

    class SuppressM115Request : ToggleMenuItem() {
        override val isEnabled get() = BaseInjector.get().octoPreferences().suppressM115Request
        override val itemId = "suppress_m115"
        override var groupId = ""
        override val canBePinned = false
        override val order = 163
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_block_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___suppress_m115_request_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().suppressM115Request = enabled
        }
    }
}