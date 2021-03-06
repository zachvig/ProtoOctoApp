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
        val endX: Float,
        val endY: Float,
        override val positionInFile: Int,
        val startX: Float,
        val startY: Float,
        val leftX: Float,
        val topY: Float,
        val r: Float,
        val startAngle: Float,
        val sweepAngle: Float,
    ) : Move(), Serializable

    enum class Type {
        Travel, Extrude, Unsupported
    }
}