package de.crysxd.octoapp.base.ui.common.menu

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import de.crysxd.octoapp.base.R

interface Menu {
    fun getMenuItem(context: Context): List<MenuItem>
}

interface MenuItem {
    val itemId: String
    val groupId: String

    val title: CharSequence
    val style: Style
    val showAsSubMenu: Boolean get() = false
    val showAsHalfWidth: Boolean get() = false

    @get:DrawableRes
    val icon: Int

    fun isVisible(@IdRes destinationId: Int) = true
    fun onClicked(host: MenuBottomSheetFragment) = true
}

sealed class Style(
    @ColorRes val backgroundColor: Int,
    @ColorRes val highlightColor: Int,
) {
    object Support : Style(R.color.red_translucent, R.color.red)
    object Settings : Style(R.color.yellow_translucent, R.color.yellow)
    object Routines : Style(R.color.green_translucent, R.color.green)
    object Printer : Style(R.color.blue_translucent, R.color.blue)
    object External : Style(R.color.light_grey_translucent, R.color.light_grey)
}