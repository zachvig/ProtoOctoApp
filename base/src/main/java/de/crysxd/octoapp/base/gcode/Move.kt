package de.crysxd.octoapp.base.gcode

data class Move(
    val positionInFile: Int,
    val fromX: Float,
    val toX: Float,
    val fromY: Float,
    val toY: Float,
    val type: Type
) {
    sealed class Type {
        object Travel : Type()
        object Extrude : Type()
    }
}