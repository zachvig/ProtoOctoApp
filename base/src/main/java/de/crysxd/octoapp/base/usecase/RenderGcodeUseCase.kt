package de.crysxd.octoapp.base.usecase

import android.graphics.*
import androidx.core.graphics.applyCanvas
import de.crysxd.octoapp.base.gcode.Move
import timber.log.Timber

class RenderGcodeUseCase : UseCase<RenderGcodeUseCase.Params, Unit>() {

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

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        // Extracts moves and translate to bitmap coordinate system
        val mmToPxFactor = param.bitmap.width / param.printBedSizeMm.x
        val scaleFactor = param.printBedSizeMm.x / param.visibleRectMm.width()
        val totalFactor = mmToPxFactor * scaleFactor
        val xOffset = -param.visibleRectMm.left
        val yOffset = -param.visibleRectMm.top

        // Calc 0.4mm line width and do not use less than 2px after the totalFactor is applied
        // Then divide by total as Canvas will apply scale later on the GPU
        extrudePaint.strokeWidth = (param.extrusionWidthMm * totalFactor).coerceAtLeast(2f) / totalFactor
        travelPaint.strokeWidth = extrudePaint.strokeWidth * 0.5f

        // Draw
        param.bitmap.applyCanvas {
            // Scale and transform so the desired are is visible
            scale(totalFactor, totalFactor)
            translate(xOffset, yOffset)

            // Background
            drawRect(0f, 0f, param.bitmap.width.toFloat(), param.bitmap.height.toFloat(), Paint().also {
                it.style = Paint.Style.FILL_AND_STROKE
                it.color = Color.WHITE
            })

            param.gcodeRenderContext.paths.forEach {
                drawLines(it.points, it.offset, it.count, it.type.paint)
            }
        }
    }

    private val Move.Type.paint
        get() = when (this) {
            Move.Type.Travel -> travelPaint
            Move.Type.Extrude -> extrudePaint
        }

    data class Params(
        val gcodeRenderContext: RenderGcodePreparationUseCase.GcodeRenderContext,
        val printBedSizeMm: PointF,
        val visibleRectMm: RectF,
        val bitmap: Bitmap,
        val extrusionWidthMm: Float = 0.4f
    )
}