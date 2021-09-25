package de.crysxd.baseui.menu

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController

interface MenuHost {
    fun requireContext(): Context
    fun pushMenu(subMenu: Menu)
    fun closeMenu()
    fun getNavController(): NavController?
    fun getMenuActivity(): FragmentActivity
    fun getMenuFragmentManager(): FragmentManager?
    fun getHostFragment(): Fragment?
    fun reloadMenu()
    fun isCheckBoxChecked(): Boolean

    fun suppressSuccessAnimationForNextAction()
    fun consumeSuccessAnimationForNextActionSuppressed(): Boolean
}