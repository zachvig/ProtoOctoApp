package de.crysxd.octoapp.base.models

import androidx.annotation.StringRes
import de.crysxd.octoapp.base.R

sealed class MenuId(@get:StringRes val label: Int) {
    object MainMenu : MenuId(R.string.menu_controls___main)
    object PrintWorkspace : MenuId(R.string.menu_controls___print_workspace)
    object PrepareWorkspace : MenuId(R.string.menu_controls___prepare_workspace)
    object Other : MenuId(R.string.menu_controls___other)
}