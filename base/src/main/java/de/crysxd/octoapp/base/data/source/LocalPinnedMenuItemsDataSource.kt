package de.crysxd.octoapp.base.data.source

import android.content.SharedPreferences
import androidx.core.content.edit
import de.crysxd.octoapp.base.data.models.MenuId
import de.crysxd.octoapp.base.data.models.MenuItems
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber


class LocalPinnedMenuItemsDataSource(
    private val sharedPreferences: SharedPreferences,
    private val octoPrintRepository: OctoPrintRepository,
) {

    fun store(menuId: MenuId, items: Set<String>) = sharedPreferences.edit {
        getKey(menuId)?.let {
            putStringSet(it, items)
        }
    }

    fun load(menuId: MenuId): Set<String>? = getKey(menuId)?.let {
        val items = sharedPreferences.getStringSet(it, null)
        return try {
            upgradeSwitchMenuItems(menuId, items)
        } catch (e: Exception) {
            Timber.e(e)
            items
        }
    }

    private fun getKey(menuId: MenuId) = when (menuId) {
        MenuId.MainMenu -> "pinned_menu_items"
        MenuId.PrePrintWorkspace -> "pinned_menu_items_preprint"
        MenuId.PrintWorkspace -> "pinned_menu_items_print"
        MenuId.Widget -> "pinned_menu_items_widget"
        MenuId.Other -> null
    }

    private fun upgradeSwitchMenuItems(menuId: MenuId, items: Set<String>?): Set<String>? {
        items ?: return null

        val legacyId = "switch___to_instance/"
        val upgraded = items.mapNotNull {
            if (it.startsWith(legacyId)) {
                val webUrl = it.replace(legacyId, "").toHttpUrl()
                val instance = octoPrintRepository.findInstancesWithWebUrl(webUrl) ?: return@mapNotNull null
                val new = "${MenuItems.MENU_ITEM_SWITCH_INSTANCE}${instance.id}"
                Timber.i("Upgrading menu item from $it -> $new")
                new
            } else {
                it
            }
        }.toSet()

        if (upgraded != items) {
            Timber.i("Upgraded menu items from $items -> $upgraded, storing now")
            store(menuId, upgraded)
        }

        return upgraded
    }
}