package de.crysxd.octoapp.octoprint.models.files

import java.io.Serializable

sealed class FileObject(
    open val display: String,
    open val name: String,
    open val origin: FileOrigin,
    open val path: String,
    open val type: String?,
    open val typePath: List<String>?,
    val size: Long,
    open val ref: Reference?
) : Serializable {

    val isPrintable get() = typePath?.contains(FILE_TYPE_MACHINE_CODE) == true

    class File(
        display: String,
        name: String,
        origin: FileOrigin,
        path: String,
        type: String?,
        typePath: List<String>?,
        ref: Reference?,
        size: Long,
        val thumbnail: String?,
        val date: Long,
        val hash: String,
        val gcodeAnalysis: GcodeAnalysis?,
        val prints: PrintHistory?
    ) : FileObject(display, name, origin, path, type, typePath, size, ref) {
        val extension get() = name.split(".").takeIf { it.size > 1 }?.lastOrNull()
    }

    class Folder(
        display: String,
        name: String,
        origin: FileOrigin,
        path: String,
        type: String,
        typePath: List<String>,
        ref: Reference,
        size: Long,
        val children: List<FileObject>?
    ) : FileObject(display, name, origin, path, type, typePath, size, ref)

    data class Reference(
        val download: String?,
        val resource: String
    ) : Serializable

    data class PrintHistory(
        val failure: Int?,
        val success: Int?,
        val last: LastPrint?
    ) : Serializable {
        data class LastPrint(
            val date: Float,
            val success: Boolean
        ) : Serializable
    }

    data class GcodeAnalysis(
        val dimensions: Dimensions?,
        val estimatedPrintTime: Float?,
        val filament: FilamentUse?,
    ) : Serializable {
        data class Dimensions(
            val depth: Double,
            val height: Double,
            val width: Double
        ) : Serializable

        data class FilamentUse(
            val tool0: ToolInfo?,
            val tool1: ToolInfo?,
        ) : Serializable {
            data class ToolInfo(
                val length: Double,
                val volume: Double
            ) : Serializable
        }
    }

    companion object {
        const val FILE_TYPE_FOLDER = "folder"
        const val FILE_TYPE_MACHINE_CODE = "machinecode"
        const val FILE_TYPE_MACHINE_CODE_GCODE = "gcode"
    }
}