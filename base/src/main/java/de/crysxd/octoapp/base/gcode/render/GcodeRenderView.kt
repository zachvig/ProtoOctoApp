package de.crysxd.octoapp.base.gcode.render

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import timber.log.Timber
import kotlin.system.measureTimeMillis

class GcodeRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var renderParams: RenderParams? = null
        set(value) {
            field = value
            invalidate()
        }

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

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private val Move.Type.paint
        get() = when (this) {
            Move.Type.Travel -> travelPaint
            Move.Type.Extrude -> extrudePaint
        }

    override fun onDraw(canvas: Canvas) = measureTimeMillis {
        super.onDraw(canvas)

        // Check HW acceleration
        if (!canvas.isHardwareAccelerated && BuildConfig.DEBUG) {
            Timber.w("Missing hardware acceleration!")
        }

        // Skip draw without params
        val params = renderParams ?: return

        // Extracts moves and translate to bitmap coordinate system
        val mmToPxFactor = width / params.printBedSizeMm.x
        val scaleFactor = params.printBedSizeMm.x / params.visibleRectMm.width()
        val totalFactor = mmToPxFactor * scaleFactor
        val xOffset = -params.visibleRectMm.left
        val yOffset = -params.visibleRectMm.top

        // Calc line width and do not use less than 2px after the totalFactor is applied
        // Then divide by total as Canvas will apply scale later on the GPU
        extrudePaint.strokeWidth = (params.extrusionWidthMm * totalFactor).coerceAtLeast(2f) / totalFactor
        travelPaint.strokeWidth = extrudePaint.strokeWidth * 0.5f

        // Scale and transform so the desired are is visible
        canvas.scale(totalFactor, totalFactor)
        canvas.translate(xOffset, yOffset)

        // Render Gcode
        params.renderContext.paths.forEach {
            canvas.drawLines(it.points, it.offset, it.count, it.type.paint)
        }
    }.let {
        if (BuildConfig.DEBUG) {
            // Do not log in non-debug builds to increase performance
            Timber.v("Render took ${it}ms")
        }
    }

    data class RenderParams(
        val renderContext: GcodeRenderContext,
        val printBedSizeMm: PointF,
        val visibleRectMm: RectF,
        val extrusionWidthMm: Float = 0.4f
    )
}