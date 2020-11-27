package de.crysxd.octoapp.base.usecase

import android.graphics.*
import androidx.core.graphics.applyCanvas
import de.crysxd.octoapp.base.gcode.Gcode
import de.crysxd.octoapp.base.gcode.Move
import timber.log.Timber

class RenderGcodeUseCase : UseCase<RenderGcodeUseCase.Params, Bitmap>() {

    private val extrudePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.BLACK
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val travelPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.GREEN
        strokeWidth = 2f
    }

    override suspend fun doExecute(param: Params, timber: Timber.Tree): Bitmap {
        // Create bitmap
        val bitmap = Bitmap.createBitmap(param.outputSizePx.x, param.outputSizePx.y, Bitmap.Config.ARGB_8888)

        // Extracts moves and translate to bitmap coordinate system
        val mmToPxFactor = bitmap.width / param.printBedSizeMm.x
        val scaleFactor = param.printBedSizeMm.x / param.visibleRectMm.width()
        val totalFactor = mmToPxFactor * scaleFactor
        val xOffset = -param.visibleRectMm.left
        val yOffset = -param.visibleRectMm.top
        fun PointF.translatePosition() = PointF(
            x,
            y
        )

        val paths = mapOf<Move.Type, Pair<Path, PointF>>(
            Move.Type.Extrude to Pair(Path(), PointF()),
            Move.Type.Travel to Pair(Path(), PointF())
        )
        param.directions.extractMoves(param.gcode).forEach {
            paths[it.type]?.let { (path, currentPosition) ->
                val from = it.from.translatePosition()
                val to = it.to.translatePosition()
                if (from != currentPosition) {
                    path.moveTo(from.x, from.y)
                }

                path.lineTo(to.x, to.y)
                currentPosition.x = to.x
                currentPosition.y = to.y
            }
        }

        // Calc 0.4mm line width and do not use less than 2px after the totalFactor is applied
        // Then divide by total as Canvas will apply scale later on the GPU
        extrudePaint.strokeWidth = (param.extrusionWidthMm * totalFactor).coerceAtLeast(2f) / totalFactor
        travelPaint.strokeWidth = extrudePaint.strokeWidth * 0.5f

        // Draw
        bitmap.applyCanvas {
            // Clip to visible area so nothing outside gets drawn
            clipRect(Rect(0, 0, width, height))

            // Scale and transform so the desired are is visible
            scale(totalFactor, totalFactor)
            translate(xOffset, yOffset)

            // Background
            drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), Paint().also {
                it.style = Paint.Style.FILL_AND_STROKE
                it.color = Color.WHITE
            })

            paths.forEach {
                drawPath(it.value.first, it.key.paint)
            }
        }

        return bitmap
    }

    private val Move.Type.paint
        get() = when (this) {
            Move.Type.Travel -> travelPaint
            Move.Type.Extrude -> extrudePaint
        }

    data class Params(
        val gcode: Gcode,
        val directions: RenderDirections,
        val printBedSizeMm: PointF,
        val visibleRectMm: RectF,
        /** View port aspect ration must match print bed aspect ratio */
        val outputSizePx: Point,
        val extrusionWidthMm: Float = 0.4f
    )

    sealed class RenderDirections {
        abstract fun extractMoves(gcode: Gcode): List<Move>

        data class ForFileLocation(val byte: Int) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): List<Move> {
                TODO("Not yet implemented")
            }

        }

        data class ForLayerProgress(val layer: Int, val progress: Float) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): List<Move> {
                val layer = gcode.layers[layer]
                val moveCount = layer.moves.size * progress.coerceIn(0f, 1f)
                return layer.moves.take(moveCount.toInt())
            }
        }
    }
}