package de.crysxd.octoapp.base.gcode.parse.models

import android.graphics.PointF
import java.io.Serializable
import kotlin.math.cos
import kotlin.math.sin

sealed class Move(
) : Serializable {
    abstract val positionInFile: Int

    data class LinearMove(
        val positionInArray: Int,
        override val positionInFile: Int,
    ) : Move(), Serializable

    data class ArcMove(
        override val positionInFile: Int,
        val leftX: Float,
        val topY: Float,
        val r: Float,
        val startAngle: Float,
        val sweepAngle: Float,
    ) : Move(), Serializable {

        val endPosition: PointF
            get() {
                val cx = leftX + r
                val cy = topY + r
                val angleRad = (startAngle + sweepAngle) * Math.PI / 180f
                val endX = cx + r * cos(angleRad)
                val endY = cy + r * sin(angleRad)
                return PointF(endX.toFloat(), endY.toFloat())
            }

    }

    enum class Type {
        Travel, Extrude, Unsupported
    }
}