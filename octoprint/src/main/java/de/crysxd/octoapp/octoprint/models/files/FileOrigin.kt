package de.crysxd.octoapp.octoprint.models.files

sealed class FileOrigin(private val name: String) {

    override fun toString() = name

    object Local : FileOrigin("local")
    object SdCard : FileOrigin("sdcard")
}