package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import android.os.Parcelable
import android.text.method.MovementMethod

interface Menu : Parcelable {
    fun getMenuItem(): List<MenuItem>
    fun getTitle(context: Context): CharSequence? = null
    fun getSubtitle(context: Context): CharSequence? = null
    fun getBottomText(context: Context): CharSequence = ""
    fun getBottomMovementMethod(host: MenuBottomSheetFragment): MovementMethod? = null
}