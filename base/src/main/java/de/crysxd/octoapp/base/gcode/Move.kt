package de.crysxd.octoapp.base.gcode

data class Move(
    val positionInFile: Int,
    val positionInLayer: Int,
    val positionInArray: Int,
    val type: Type
) {
    sealed class Type {
        object Travel : Type()
        object Extrude : Type()
    }
}