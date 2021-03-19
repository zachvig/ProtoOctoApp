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
        RotationMenuItem()
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.lab_menu___subtitle)

    class RotationMenuItem : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().allowAppRotation
        override val itemId = ""
        override var groupId = ""
        override val order = 1
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_screen_rotation_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.lab_menu___allow_to_rotate_description)
        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
            Injector.get().octoPreferences().allowAppRotation = enabled
        }
    }
}