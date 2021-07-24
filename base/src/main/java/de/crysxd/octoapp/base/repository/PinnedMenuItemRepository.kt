package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.LocalPinnedMenuItemsDataSource
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.menu.main.*


class PinnedMenuItemRepository(
    private val dataSource: LocalPinnedMenuItemsDataSource
) {

    private val defaults = setOf(
        MENU_ITEM_OPEN_OCTOPRINT,
        MENU_ITEM_OPEN_TERMINAL,
        MENU_ITEM_CANCEL_PRINT,
        MENU_ITEM_SHOW_FILES,
        MENU_ITEM_CONFIGURE_REMOTE_ACCESS,
        MENU_ITEM_CUSTOMIZE_WIDGETS,
        MENU_ITEM_EMERGENCY_STOP,
        MENU_ITEM_TURN_PSU_OFF,
        MENU_ITEM_HELP
    )

    fun toggleMenuItemPinned(menuId: MenuId, itemId: String) {
        val data = (dataSource.load(menuId) ?: defaults).toMutableList()
        if (data.contains(itemId)) {
            data.remove(itemId)
        } else {
            data.add(itemId)
        }
        dataSource.store(menuId, data.toSet())
    }

    fun getPinnedMenuItems(menuId: MenuId) = dataSource.load(menuId) ?: defaults
}