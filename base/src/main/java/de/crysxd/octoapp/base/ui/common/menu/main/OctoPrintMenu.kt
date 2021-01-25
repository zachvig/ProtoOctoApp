package de.crysxd.octoapp.base.ui.common.menu.main

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.menu.Menu
import de.crysxd.octoapp.base.ui.common.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.common.menu.MenuItem
import de.crysxd.octoapp.base.ui.common.menu.MenuItemStyle
import kotlinx.android.parcel.Parcelize

@Parcelize
class OctoPrintMenu : Menu {
    override fun getMenuItem() = listOf(
        OpenOctoPrintMenuItem()
    )

    override fun getTitle(context: Context) = "OctoPrint"
    override fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
}

class OpenOctoPrintMenuItem : MenuItem {
    override val itemId = MENU_ITEM_OPEN_OCTOPRINT
    override var groupId = ""
    override val order = 400
    override val enforceSingleLine = false
    override val style = MenuItemStyle.OctoPrint
    override val icon = R.drawable.ic_round_open_in_browser_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_octoprint)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().openOctoPrintWebUseCase().execute(host.requireContext())
        return true
    }
}
