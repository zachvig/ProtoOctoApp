package de.crysxd.octoapp.octoprint.models.files

sealed class FileCommand(val command: String) {

    data class SelectFile(val print: Boolean = false) : FileCommand("select")

    data class MoveFile(val destination: String) : FileCommand("move")

    data class CopyFile(val destination: String) : FileCommand("copy")

}