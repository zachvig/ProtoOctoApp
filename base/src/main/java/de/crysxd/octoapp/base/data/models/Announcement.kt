package de.crysxd.octoapp.base.data.models

import android.content.Context
import android.net.Uri
import androidx.annotation.ColorRes
import de.crysxd.octoapp.base.R

data class Announcement(
    val text: Context.() -> CharSequence,
    val refreshInterval: Long = 0,
    val actionText: Context.() -> CharSequence?,
    val actionUri: Context.() -> Uri?,
    val id: String,
    val canHide: Boolean = true,
    @ColorRes val backgroundColor: Int = R.color.input_background,
    @ColorRes val foregroundColor: Int = R.color.accent,
)
