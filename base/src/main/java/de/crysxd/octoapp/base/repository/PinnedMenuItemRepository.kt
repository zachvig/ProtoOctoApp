package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.DataSource

class PinnedMenuItemRepository(
    private val dataSource: DataSource<Set<String>>
) {

    private val defaults = setOf("cancel_print", "emergency_stop", "turn_psu_off")

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