package de.crysxd.octoapp.help.faq

import android.content.Context
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle

class HelpMenuItem(override val style: MenuItemStyle, private val title: CharSequence, private val onClick: () -> Unit) : MenuItem {
    override val itemId: String = ""
    override var groupId: String = ""
    override val order: Int = 0
    override val icon: Int = 0
    override val showAsSubMenu = true

    override fun getTitle(context: Context) = title
    override suspend fun onClicked(host: MenuHost?) = onClick()
}