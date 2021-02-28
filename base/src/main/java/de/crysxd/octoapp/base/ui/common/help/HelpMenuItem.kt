package de.crysxd.octoapp.base.ui.common.help

import android.content.Context
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle

class HelpMenuItem(override val style: MenuItemStyle, private val title: CharSequence, private val onClick: () -> Unit) : MenuItem {
    override val itemId: String = ""
    override var groupId: String = ""
    override val order: Int = 0
    override val icon: Int = 0
    override val showAsSubMenu = true

    override suspend fun getTitle(context: Context) = title
    override suspend fun onClicked(host: MenuBottomSheetFragment?) = onClick()
}