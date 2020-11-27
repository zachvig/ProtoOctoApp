package de.crysxd.octoapp.base.gcode

import android.graphics.PointF

data class Move(
    val positionInFile: Int,
    val positionInLayer: Int,
    val from: PointF,
    val to: PointF,
    val type: Type
) {
    sealed class Type {
        object Travel : Type()
        object Extrude : Type()
    }
}