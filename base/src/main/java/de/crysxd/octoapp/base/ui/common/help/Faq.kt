package de.crysxd.octoapp.base.ui.common.help

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Faq(
    val title: String?,
    val content: String?,
    val youtubeUrl: String?,
    val youtubeThumbnailUrl: String?,
) : Parcelable