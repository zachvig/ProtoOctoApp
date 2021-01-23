package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_CANCEL_PRINT
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_EMERGENCY_STOP
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_SEND_FEEDBACK
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_TURN_PSU_OFF


class PinnedMenuItemRepository(
    private val dataSource: DataSource<Set<String>>
) {

    private val defaults = setOf(MENU_ITEM_SEND_FEEDBACK, MENU_ITEM_CANCEL_PRINT, MENU_ITEM_EMERGENCY_STOP, MENU_ITEM_TURN_PSU_OFF)

    fun toggleMenuItemPinned(itemId: String) {
        val data = (dataSource.get() ?: defaults).toMutableList()
        if (data.contains(itemId)) {
            data.remove(itemId)
        } else {
            data.add(itemId)
        }
        dataSource.store(data.toSet())
    }

    fun getPinnedMenuItems() = dataSource.get() ?: defaults

}