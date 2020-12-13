package de.crysxd.octoapp.base.gcode.render

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.minus
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import timber.log.Timber
import kotlin.math.max
import kotlin.system.measureTimeMillis

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 10f
private const val ZOOM_SPEED = 0.2f
private const val DOUBLE_TAP_ZOOM = 5f

class GcodeRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())

    private val minPrintHeadDiameter = resources.getDimension(R.dimen.gcode_render_view_print_head_size)
    private var scrollOffset = PointF(0f, 0f)
    private var zoom = 1f
    private var printBed: Drawable? = null
    var renderParams: RenderParams? = null
        set(value) {
            printBed = value?.let {
                ContextCompat.getDrawable(context, it.renderStyle.background)
            }
            field = value
            invalidate()
        }

    private val paddedHeight
        get() = height - paddingTop - paddingBottom

    private val paddedWidth
        get() = width - paddingLeft - paddingRight

    private val RenderParams.mmToPxFactor
        get() = paddedWidth / printBedSizeMm.x

    private val RenderParams.pxToMmFactor
        get() = printBedSizeMm.x / paddedWidth

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
        val style = params.renderStyle

        // Enforce scroll limits
        enforceScrollLimits(params)

        // Calc offsets, center render if smaller than view
        val backgroundWidth = paddedWidth.toFloat()
        val backgroundHeight = printBed?.let {
            it.intrinsicHeight * (backgroundWidth / it.intrinsicWidth)
        } ?: paddedHeight.toFloat()
        val totalFactor = params.mmToPxFactor * zoom
        val printBedWidthPx = params.printBedSizeMm.x * totalFactor
        val printBedHeightPx = params.printBedSizeMm.y * totalFactor
        val xOffset = if (printBedWidthPx < width) {
            ((width - max(printBedWidthPx, backgroundWidth)) / 2)
        } else {
            scrollOffset.x
        }
        val yOffset = if (printBedHeightPx < height) {
            ((height - max(printBedHeightPx, backgroundHeight)) / 2)
        } else {
            scrollOffset.y
        }

        // Calc line width and do not use less than 2px after the totalFactor is applied
        // Then divide by total as Canvas will apply scale later on the GPU
        val strokeWidth = (params.extrusionWidthMm * totalFactor * 0.8f).coerceAtLeast(2f) / totalFactor
        style.paintPalette(Move.Type.Extrude).strokeWidth = strokeWidth
        style.paintPalette(Move.Type.Travel).strokeWidth = strokeWidth * 0.5f

        // Scale and transform so the desired are is visible
        canvas.save()
        canvas.translate(xOffset, yOffset)
        canvas.scale(totalFactor, totalFactor)

        // Draw background
        printBed?.setBounds(
            0,
            0,
            (backgroundWidth * params.pxToMmFactor).toInt(),
            (backgroundHeight * params.pxToMmFactor).toInt()
        )
        printBed?.draw(canvas)

        // Apply same scale but mirror vertically as view's origin is top left and printer's is bottom left
        canvas.restore()
        canvas.translate(xOffset, yOffset + printBedHeightPx)
        canvas.scale(totalFactor, -totalFactor)

        // Render Gcode
        params.renderContext.paths.forEach {
            canvas.drawLines(it.points, it.offset, it.count, style.paintPalette(it.type))
        }

        // Tool position
        params.renderContext.printHeadPosition?.let {
            val actualRadius = params.extrusionWidthMm * 3
            val minRadius = minPrintHeadDiameter / totalFactor
            val radius = max(actualRadius, minRadius)
            canvas.drawOval(
                it.x - radius,
                it.y - radius,
                it.x + radius,
                it.y + radius,
                style.printHeadPaint
            )

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
        val renderStyle: RenderStyle,
        val printBedSizeMm: PointF,
        val extrusionWidthMm: Float = 0.5f,
    )
}