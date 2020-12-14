package de.crysxd.octoapp.base.models

data class GcodeHistoryItem(
    val command: String,
    val lastUsed: Long = 0,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val label: String? = null
) {

    val oneLineCommand get() = command.replace("\n", " | ")
    val name get() = label ?: oneLineCommand

}