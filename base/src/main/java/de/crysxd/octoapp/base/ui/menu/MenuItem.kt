package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes

typealias SuspendExecutor = suspend (suspend () -> Unit) -> Unit

interface MenuItem {
    val itemId: String
    var groupId: String
    val order: Int

    val style: MenuItemStyle
    val showAsSubMenu: Boolean get() = false
    val showAsHalfWidth: Boolean get() = false
    val canBePinned: Boolean get() = true
    val enforceSingleLine: Boolean get() = true
    val secondaryButtonIcon: Int? get() = null


    @get:DrawableRes
    val icon: Int

    suspend fun getTitle(context: Context): CharSequence
    suspend fun getRightDetail(context: Context): CharSequence? = null
    suspend fun getDescription(context: Context): CharSequence? = null
    suspend fun isVisible(@IdRes destinationId: Int) = true
    suspend fun onClicked(host: MenuHost?)
    suspend fun onSecondaryClicked(host: MenuHost?) = Unit
}
