package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit

private const val KEY_PINNED_MENU_ITEM = "pinned_menu_items"

class LocalPinnedMenuItemsDataSource(private val sharedPreferences: SharedPreferences) : DataSource<Set<String>> {

    override fun store(t: Set<String>?) {
        sharedPreferences.edit {
            putStringSet(KEY_PINNED_MENU_ITEM, t?.toSet())
        }
    }

    override fun get() = sharedPreferences.getStringSet(KEY_PINNED_MENU_ITEM, null)

}