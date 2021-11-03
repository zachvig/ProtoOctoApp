package de.crysxd.octoapp.base.data.models

import android.net.Uri
import androidx.annotation.ColorRes
import de.crysxd.octoapp.base.R

data class Announcement(
    val text: () -> CharSequence,
    val refreshInterval: Long = 0,
    val actionText: CharSequence?,
    val actionUri: Uri?,
    val id: String,
    @ColorRes val backgroundColor: Int = R.color.input_background,
    @ColorRes val foregroundColor: Int = R.color.accent,
)
