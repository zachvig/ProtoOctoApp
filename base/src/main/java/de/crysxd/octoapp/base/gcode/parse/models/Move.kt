package de.crysxd.octoapp.base.gcode.parse.models

import java.io.Serializable

sealed class Move(
) : Serializable {
    abstract val positionInFile: Int

    data class LinearMove(
        val positionInArray: Int,
        override val positionInFile: Int,
    ) : Move(), Serializable

    data class ArcMove(
        val arc: Arc,
        val endX: Float,
        val endY: Float,
        override val positionInFile: Int,
    ) : Move(), Serializable

    data class Arc(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val leftX: Float,
        val topY: Float,
        val r: Float,
        val startAngle: Float,
        val sweepAngle: Float,
    ) : Serializable

    enum class Type {
        Travel, Extrude, Unsupported
    }
}