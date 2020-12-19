package de.crysxd.octoapp.base.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GcodeHistoryItem(
    val command: String,
    val lastUsed: Long = 0,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val label: String? = null
) : Parcelable {

    val oneLineCommand get() = command.replace("\n", " | ")
    val name get() = label ?: oneLineCommand

}