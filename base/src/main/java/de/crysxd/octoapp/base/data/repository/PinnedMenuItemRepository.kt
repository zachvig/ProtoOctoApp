package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.data.models.MenuId
import de.crysxd.octoapp.base.data.models.MenuItems
import de.crysxd.octoapp.base.data.source.LocalPinnedMenuItemsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class PinnedMenuItemRepository(
    private val dataSource: LocalPinnedMenuItemsDataSource
) {

    private val defaults = mapOf(
        MenuId.MainMenu to setOf(
            MenuItems.MENU_ITEM_OPEN_OCTOPRINT,
            MenuItems.MENU_ITEM_OPEN_TERMINAL,
            MenuItems.MENU_ITEM_CANCEL_PRINT,
            MenuItems.MENU_ITEM_SHOW_FILES,
            MenuItems.MENU_ITEM_CONFIGURE_REMOTE_ACCESS,
            MenuItems.MENU_ITEM_CUSTOMIZE_WIDGETS,
            MenuItems.MENU_ITEM_EMERGENCY_STOP,
            MenuItems.MENU_ITEM_TURN_PSU_OFF,
            MenuItems.MENU_ITEM_HELP,
            MenuItems.MENU_ITEM_PLUGINS
        ),
        MenuId.PrePrintWorkspace to setOf(
            MenuItems.MENU_ITEM_OPEN_OCTOPRINT,
            MenuItems.MENU_ITEM_SHOW_FILES,
            MenuItems.MENU_ITEM_CUSTOMIZE_WIDGETS,
            MenuItems.MENU_ITEM_HELP
        ),
        MenuId.PrintWorkspace to setOf(
            MenuItems.MENU_ITEM_OPEN_OCTOPRINT,
            MenuItems.MENU_ITEM_OPEN_TERMINAL,
            MenuItems.MENU_ITEM_HELP
        ),
        MenuId.Widget to setOf(
            MenuItems.MENU_ITEM_OPEN_OCTOPRINT,
            MenuItems.MENU_ITEM_SHOW_FILES,
            MenuItems.MENU_ITEM_HELP
        )
    )
    private val flows = mutableMapOf<MenuId, MutableStateFlow<Set<String>>>()

    fun checkPinnedState(itemId: String) = MenuId.values().filter {
        // We can't store other
        it != MenuId.Other
    }.map {
        it to getPinnedMenuItems(it).contains(itemId)
    }

    fun toggleMenuItemPinned(menuId: MenuId, itemId: String) {
        val data = (dataSource.load(menuId) ?: defaults[menuId] ?: emptySet()).toMutableList()
        if (data.contains(itemId)) {
            data.remove(itemId)
        } else {
            data.add(itemId)
        }
        dataSource.store(menuId, data.toSet())
        getChannel(menuId).value = getPinnedMenuItems(menuId)
    }

    fun getPinnedMenuItems(menuId: MenuId) = dataSource.load(menuId) ?: defaults[menuId] ?: emptySet()

    private fun getChannel(menuId: MenuId) = flows.getOrPut(menuId) {
        MutableStateFlow(getPinnedMenuItems(menuId))
    }

    fun observePinnedMenuItems(menuId: MenuId) = getChannel(menuId).asStateFlow()
}