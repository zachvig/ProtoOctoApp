package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.LocalPinnedMenuItemsDataSource
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_CANCEL_PRINT
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_CONFIGURE_REMOTE_ACCESS
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_CUSTOMIZE_WIDGETS
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_EMERGENCY_STOP
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_HELP
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_OPEN_OCTOPRINT
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_OPEN_TERMINAL
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_SHOW_FILES
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_TURN_PSU_OFF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PinnedMenuItemRepository(
    private val dataSource: LocalPinnedMenuItemsDataSource
) {

    private val defaults = mapOf(
        MenuId.MainMenu to setOf(
            MENU_ITEM_OPEN_OCTOPRINT,
            MENU_ITEM_OPEN_TERMINAL,
            MENU_ITEM_CANCEL_PRINT,
            MENU_ITEM_SHOW_FILES,
            MENU_ITEM_CONFIGURE_REMOTE_ACCESS,
            MENU_ITEM_CUSTOMIZE_WIDGETS,
            MENU_ITEM_EMERGENCY_STOP,
            MENU_ITEM_TURN_PSU_OFF,
            MENU_ITEM_HELP
        ),
        MenuId.PrePrintWorkspace to setOf(
            MENU_ITEM_OPEN_OCTOPRINT,
            MENU_ITEM_SHOW_FILES,
            MENU_ITEM_CUSTOMIZE_WIDGETS,
            MENU_ITEM_HELP
        ),
        MenuId.PrintWorkspace to setOf(
            MENU_ITEM_OPEN_OCTOPRINT,
            MENU_ITEM_OPEN_TERMINAL,
            MENU_ITEM_HELP
        ),
        MenuId.Widget to setOf(
            MENU_ITEM_OPEN_OCTOPRINT,
            MENU_ITEM_SHOW_FILES,
            MENU_ITEM_HELP
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