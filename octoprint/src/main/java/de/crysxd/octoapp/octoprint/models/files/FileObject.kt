package de.crysxd.octoapp.octoprint.models.files

import java.io.Serializable

sealed class FileObject(
    open val display: String,
    open val name: String,
    open val origin: String,
    open val path: String,
    open val type: String,
    open val typePath: List<String>,
    val size: Long,
    open val ref: Reference
) {

    class File(
        display: String,
        name: String,
        origin: String,
        path: String,
        type: String,
        typePath: List<String>,
        ref: Reference,
        size: Long,
        val thumbnail: String?,
        val date: Long
    ) : FileObject(display, name, origin, path, type, typePath, size, ref), Serializable

    class Folder(
        display: String,
        name: String,
        origin: String,
        path: String,
        type: String,
        typePath: List<String>,
        ref: Reference,
        size: Long,
        val children: List<FileObject>?
    ) : FileObject(display, name, origin, path, type, typePath, size, ref), Serializable

    data class Reference(
        val download: String?,
        val resource: String
    )

    companion object {
        const val FILE_TYPE_FOLDER = "folder"
        const val FILE_TYPE_MACHINE_CODE= "machinecode"
        const val FILE_TYPE_MACHINE_CODE_GCODE= "gcode"
    }
}