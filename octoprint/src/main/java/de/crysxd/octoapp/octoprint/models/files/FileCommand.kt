package de.crysxd.octoapp.octoprint.models.files

sealed class FileCommand(val command: String) {

    data class SelectFile(val print: Boolean = false) : FileCommand("select")

}