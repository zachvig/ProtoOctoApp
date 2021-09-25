package de.crysxd.baseui.menu

import androidx.annotation.ColorRes
import de.crysxd.baseui.R

sealed class MenuItemStyle(
    @ColorRes val backgroundColor: Int,
    @ColorRes val highlightColor: Int,
) {
    // We have similar mapping in QuickAccessRemoteViewsFactory!

    object Red : MenuItemStyle(R.color.menu_style_support_background, R.color.menu_style_support_foreground)
    object Yellow : MenuItemStyle(R.color.menu_style_settings_background, R.color.menu_style_settings_foreground)
    object Green : MenuItemStyle(R.color.menu_style_octoprint_background, R.color.menu_style_octoprint_foreground)
    object Blue : MenuItemStyle(R.color.menu_style_printer_background, R.color.menu_style_printer_foreground)

    object Support : MenuItemStyle(R.color.menu_style_support_background, R.color.menu_style_support_foreground)
    object Settings : MenuItemStyle(R.color.menu_style_settings_background, R.color.menu_style_settings_foreground)
    object OctoPrint : MenuItemStyle(R.color.menu_style_octoprint_background, R.color.menu_style_octoprint_foreground)
    object Printer : MenuItemStyle(R.color.menu_style_printer_background, R.color.menu_style_printer_foreground)

    object Neutral : MenuItemStyle(R.color.menu_style_neutral_background, R.color.menu_style_neutral_foreground)
    object RedNeutral : MenuItemStyle(R.color.menu_style_support_background, R.color.menu_style_neutral_foreground)
}