package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import android.os.Parcelable
import android.text.method.MovementMethod

interface Menu : Parcelable {
    suspend fun getMenuItem(): List<MenuItem>
    suspend fun beforeShow(host: MenuBottomSheetFragment) = false
    suspend fun getTitle(context: Context): CharSequence? = null
    suspend fun getSubtitle(context: Context): CharSequence? = null
    fun getEmptyStateIcon(): Int = 0
    fun getEmptyStateActionText(context: Context): String? = null
    fun getEmptyStateActionUrl(context: Context): String? = null
    fun getEmptyStateSubtitle(context: Context): CharSequence? = null
    fun getCheckBoxText(context: Context): CharSequence? = null
    fun getBottomText(context: Context): CharSequence = ""
    fun getBottomMovementMethod(host: MenuBottomSheetFragment): MovementMethod? = null
}