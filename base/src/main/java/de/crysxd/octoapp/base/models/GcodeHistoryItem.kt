package de.crysxd.octoapp.base.models

data class GcodeHistoryItem(
    val command: String,
    val lastUsed: Long = 0,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)