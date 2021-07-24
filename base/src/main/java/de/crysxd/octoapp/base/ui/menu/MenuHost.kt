package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment

interface MenuHost {
    fun requireContext(): Context
    fun pushMenu(subMenu: Menu)
    fun closeMenu()
    fun getNavController(): NavController
    fun getOctoActivity(): OctoActivity
    fun getFragmentManager(): FragmentManager?
    fun getWidgetHostFragment(): WidgetHostFragment?
    fun reloadMenu()
    fun isCheckBoxChecked(): Boolean
}