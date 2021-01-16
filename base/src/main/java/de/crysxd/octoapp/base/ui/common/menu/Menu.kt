package de.crysxd.octoapp.base.ui.common.menu

import android.content.Context
import android.os.Parcelable

interface Menu : Parcelable {
    fun getMenuItem(): List<MenuItem>
    fun getTitle(context: Context): CharSequence? = null
    fun getSubtitle(context: Context): CharSequence? = null
    fun getBottomText(context: Context): CharSequence? = null
}