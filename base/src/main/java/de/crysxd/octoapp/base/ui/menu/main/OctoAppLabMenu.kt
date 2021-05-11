package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.ToggleMenuItem
import kotlinx.parcelize.Parcelize

@Parcelize
class OctoAppLabMenu : Menu {
    override suspend fun getMenuItem() = listOf(
        RotationMenuItem(),
        NotificationBatterySaver(),
        ExperimentalWebcam()
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
        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
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
        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
            Injector.get().octoPreferences().allowAppRotation = enabled
        }
    }

    class ExperimentalWebcam : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().experimentalWebcam
        override val itemId = "experimental_webcam"
        override var groupId = ""
        override val canBePinned = false
        override val order = 2
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_videocam_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___experimental_webcam_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___experimental_webcam_description)
        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
            Injector.get().octoPreferences().experimentalWebcam = enabled
        }
    }
}