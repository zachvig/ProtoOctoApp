package de.crysxd.octoapp.base.ui.common.menu

import android.content.Context

interface Menu {
    fun getMenuItem(): List<MenuItem>
    fun getTitle(context: Context): CharSequence? = null
    fun getSubtitle(context: Context): CharSequence? = null
    fun getBottomText(context: Context): CharSequence? = null
}