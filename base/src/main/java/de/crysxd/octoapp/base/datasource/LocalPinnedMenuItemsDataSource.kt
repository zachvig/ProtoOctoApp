package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import de.crysxd.octoapp.base.models.MenuId


class LocalPinnedMenuItemsDataSource(private val sharedPreferences: SharedPreferences) {

    fun store(menuId: MenuId, items: Set<String>) = sharedPreferences.edit {
        putStringSet(getKey(menuId), items)
    }

    fun load(menuId: MenuId): Set<String>? = sharedPreferences.getStringSet(getKey(menuId), null)

    private fun getKey(menuId: MenuId) = when (menuId) {
        MenuId.MainMenu -> "pinned_menu_items"
        MenuId.PrePrintWorkspace -> "pinned_menu_items_preprint"
        MenuId.PrintWorkspace -> "pinned_menu_items_print"
        MenuId.Widget -> "pinned_menu_items_widget"
        MenuId.Other -> throw IllegalArgumentException("Can't store MenuId.Other!")
    }
}