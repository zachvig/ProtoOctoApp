package de.crysxd.octoapp.base.gcode.render

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.minus
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import timber.log.Timber
import kotlin.system.measureTimeMillis

private const val MIN_ZOOM = 0.9f
private const val MAX_ZOOM = 10f
private const val ZOOM_SPEED = 0.4f
private const val DOUBLE_TAP_ZOOM = 5f

class GcodeRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())

    private var scrollOffset = PointF(0f, 0f)
    private var zoom = 0.9f
    var renderParams: RenderParams? = null
        set(value) {
            field = value
            invalidate()
        }

    private val extrudePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.LTGRAY
        strokeCap = Paint.Cap.ROUND
    }

    private val travelPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.GREEN
        strokeCap = Paint.Cap.ROUND
    }

    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.LTGRAY
        strokeWidth = 2 * (context.resources.displayMetrics.densityDpi / 160f)
        strokeCap = Paint.Cap.ROUND
    }

    private val Move.Type.paint
        get() = when (this) {
            Move.Type.Travel -> travelPaint
            Move.Type.Extrude -> extrudePaint
        }

    private val RenderParams.mmToPxFactor
        get() = width / printBedSizeMm.x

    private val RenderParams.pxToMmFactor
        get() = printBedSizeMm.x / width

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scrollOffset.x = -w / 2f
        scrollOffset.y = -h / 2f
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    private fun enforceScrollLimits(params: RenderParams) {
        val minXOffset = width - (params.printBedSizeMm.x * params.mmToPxFactor * zoom)
        val minYOffset = height - (params.printBedSizeMm.y * params.mmToPxFactor * zoom)

        scrollOffset.x = scrollOffset.x.coerceAtLeast(minXOffset).coerceAtMost(0f)
        scrollOffset.y = scrollOffset.y.coerceAtLeast(minYOffset).coerceAtMost(0f)
    }

    private fun animateZoom(focusX: Float, focusY: Float, newZoom: Float) {
        ObjectAnimator.ofFloat(zoom, newZoom).also {
            it.addUpdateListener {
                zoom(focusX, focusY, it.animatedValue as Float)
            }
            it.interpolator = DecelerateInterpolator()
            it.duration = 150
            it.start()
        }
    }

    private fun zoom(focusX: Float, focusY: Float, newZoom: Float) {
        fun fromViewToPrinter(params: RenderParams, x: Float, y: Float) = PointF(
            (x + -scrollOffset.x) * (params.pxToMmFactor / zoom),
            (y + -scrollOffset.y) * (params.pxToMmFactor / zoom),
        )

        renderParams?.let {
            val focusOnPrinterBefore = fromViewToPrinter(it, focusX, focusY)
            zoom = newZoom
            val focusOnPrinterAfter = fromViewToPrinter(it, focusX, focusY)
            val offset = focusOnPrinterAfter - focusOnPrinterBefore
            scrollOffset.x += offset.x * it.mmToPxFactor * zoom
            scrollOffset.y += offset.y * it.mmToPxFactor * zoom
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) = measureTimeMillis {
        super.onDraw(canvas)

        // Check HW acceleration
        if (!canvas.isHardwareAccelerated && BuildConfig.DEBUG) {
            Timber.w("Missing hardware acceleration!")
        }

        // Skip draw without params
        val params = renderParams ?: return

        // Enforce scroll limits
        enforceScrollLimits(params)

        // Calc offsets, center render if smaller than view
        val totalFactor = params.mmToPxFactor * zoom
        val printBedWidthPx = params.printBedSizeMm.x * totalFactor
        val printBedHeightPx = params.printBedSizeMm.y * totalFactor
        val xOffset = if (printBedWidthPx < width) {
            ((width - printBedWidthPx) / 2)
        } else {
            scrollOffset.x
        }
        val yOffset = if (printBedHeightPx < height) {
            ((height - printBedHeightPx) / 2)
        } else {
            scrollOffset.y
        }

        // Calc line width and do not use less than 2px after the totalFactor is applied
        // Then divide by total as Canvas will apply scale later on the GPU
        extrudePaint.strokeWidth = (params.extrusionWidthMm * totalFactor * 0.8f).coerceAtLeast(2f) / totalFactor
        travelPaint.strokeWidth = extrudePaint.strokeWidth * 0.5f

        // Scale and transform so the desired are is visible
        canvas.translate(xOffset, yOffset)
        canvas.scale(totalFactor, totalFactor)

        // Draw background
        params.background?.setBounds(0, 0, params.printBedSizeMm.x.toInt(), params.printBedSizeMm.y.toInt())
        params.background?.draw(canvas)

        // Draw grid
//        val scaledGridPaintStrokeWidth = (gridPaint.strokeWidth / totalFactor) / 2f
//        canvas.drawRect(
//            scaledGridPaintStrokeWidth,
//            scaledGridPaintStrokeWidth,
//            params.printBedSizeMm.x - scaledGridPaintStrokeWidth,
//            params.printBedSizeMm.y - scaledGridPaintStrokeWidth,
//            gridPaint
//        )

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

    inner class ScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Check if we increase or decrease zoom
            val scaleDirection = if (detector.previousSpan > detector.currentSpan) -1f else 1f
            val zoomChange = detector.scaleFactor * ZOOM_SPEED * scaleDirection
            val newZoom = (zoom + zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
            zoom(detector.focusX, detector.focusY, newZoom)
            invalidate()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector) = true

        override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) = Unit

        override fun onSingleTapUp(e: MotionEvent) = false

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            renderParams?.let {
                scrollOffset.x -= distanceX
                scrollOffset.y -= distanceY
                invalidate()
            }

            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val newZoom = if (zoom > 1) {
                1f
            } else {
                DOUBLE_TAP_ZOOM
            }
            animateZoom(e.x, e.y, newZoom)

            return true
        }

        override fun onLongPress(e: MotionEvent?) = Unit

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) = false

    }

    data class RenderParams(
        val renderContext: GcodeRenderContext,
        val printBedSizeMm: PointF,
        val extrusionWidthMm: Float = 0.5f,
        val background: Drawable?
    )
}