package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.LocalPinnedMenuItemsDataSource
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.menu.main.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow

@Suppress("EXPERIMENTAL_API_USAGE")
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
        )
    )
    private val channels = mutableMapOf<MenuId, ConflatedBroadcastChannel<Set<String>>>()

    fun checkPinnedState(itemId: String) = listOf(
        MenuId.MainMenu to getPinnedMenuItems(MenuId.MainMenu).contains(itemId),
        MenuId.PrePrintWorkspace to getPinnedMenuItems(MenuId.PrePrintWorkspace).contains(itemId),
        MenuId.PrintWorkspace to getPinnedMenuItems(MenuId.PrintWorkspace).contains(itemId),
    )

    fun toggleMenuItemPinned(menuId: MenuId, itemId: String) {
        val data = (dataSource.load(menuId) ?: defaults[menuId] ?: emptySet()).toMutableList()
        if (data.contains(itemId)) {
            data.remove(itemId)
        } else {
            data.add(itemId)
        }
        dataSource.store(menuId, data.toSet())
        getChannel(menuId).offer(getPinnedMenuItems(menuId))
    }

    fun getPinnedMenuItems(menuId: MenuId) = dataSource.load(menuId) ?: defaults[menuId] ?: emptySet()

    private fun getChannel(menuId: MenuId) = channels.getOrPut(menuId) {
        ConflatedBroadcastChannel(getPinnedMenuItems(menuId))
    }

    fun observePinnedMenuItems(menuId: MenuId) = getChannel(menuId).asFlow()
}