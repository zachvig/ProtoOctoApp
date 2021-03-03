package de.crysxd.octoapp.base.ui.common.help

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class KnownBug(
    val title: String?,
    val status: String?,
    val content: String?
) : Parcelable