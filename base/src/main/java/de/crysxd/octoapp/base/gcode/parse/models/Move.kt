package de.crysxd.octoapp.base.gcode.parse.models

import java.io.Serializable

data class Move(
    val positionInFile: Int,
    val positionInLayer: Int,
    val positionInArray: Int,
    val type: Type
) : Serializable {
    enum class Type {
        Travel, Extrude
    }
}