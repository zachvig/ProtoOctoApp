package de.crysxd.baseui.menu

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes

interface MenuItem {
    val itemId: String
    var groupId: String
    val order: Int

    val style: MenuItemStyle
    val showAsSubMenu: Boolean get() = false
    val showAsHalfWidth: Boolean get() = false
    val showAsOutlined: Boolean get() = showAsHalfWidth
    val canBePinned: Boolean get() = true
    val enforceSingleLine: Boolean get() = true
    val secondaryButtonIcon: Int? get() = null
    val canRunWithAppInBackground: Boolean get() = true

    @get:DrawableRes
    val icon: Int

    suspend fun onClicked(host: MenuHost?)
    suspend fun onSecondaryClicked(host: MenuHost?) = Unit

    fun getTitle(context: Context): CharSequence
    fun getRightDetail(context: Context): CharSequence? = null
    fun getDescription(context: Context): CharSequence? = null
    fun isVisible(@IdRes destinationId: Int) = true
    fun isEnabled(@IdRes destinationId: Int) = true
    fun getBadgeCount() = 0
}
