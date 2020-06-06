package de.crysxd.octoapp.octoprint.models.files

sealed class FileObject(
    open val display: String,
    open val name: String,
    open val origin: String,
    open val path: String,
    open val type: String,
    open val typePath: List<String>,
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
        val size: Long,
        val date: Long
    ) : FileObject(display, name, origin, path, type, typePath, ref)

    class Folder(
        display: String,
        name: String,
        origin: String,
        path: String,
        type: String,
        typePath: List<String>,
        ref: Reference
    ) : FileObject(display, name, origin, path, type, typePath, ref)

    data class Reference(
        val download: String?,
        val resource: String
    )
}