package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import android.os.Parcelable
import android.text.method.MovementMethod

interface Menu : Parcelable {
    fun shouldLoadBlocking() = false
    suspend fun getMenuItem(): List<MenuItem>
    suspend fun shouldShowMenu(host: MenuHost) = true
    suspend fun getTitle(context: Context): CharSequence? = null
    suspend fun getSubtitle(context: Context): CharSequence? = null
    fun getEmptyStateIcon(): Int = 0
    fun getEmptyStateActionText(context: Context): String? = null
    fun getEmptyStateActionUrl(context: Context): String? = null
    fun getEmptyStateSubtitle(context: Context): CharSequence? = null
    fun getCheckBoxText(context: Context): CharSequence? = null
    fun getBottomText(context: Context): CharSequence? = null
    fun getBottomMovementMethod(host: MenuHost): MovementMethod? = null
}