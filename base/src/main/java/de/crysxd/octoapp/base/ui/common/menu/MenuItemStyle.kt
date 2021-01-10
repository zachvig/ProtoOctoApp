package de.crysxd.octoapp.base.ui.common.menu

import androidx.annotation.ColorRes
import de.crysxd.octoapp.base.R

sealed class MenuItemStyle(
    @ColorRes val backgroundColor: Int,
    @ColorRes val highlightColor: Int,
) {
    object Support : MenuItemStyle(R.color.red_translucent, R.color.red)
    object Settings : MenuItemStyle(R.color.yellow_translucent, R.color.yellow)
    object Routines : MenuItemStyle(R.color.green_translucent, R.color.green)
    object Printer : MenuItemStyle(R.color.blue_translucent, R.color.blue)
    object Neutral : MenuItemStyle(R.color.light_grey_translucent, R.color.light_grey)
}