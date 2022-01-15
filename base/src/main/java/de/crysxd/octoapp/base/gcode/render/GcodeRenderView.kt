package de.crysxd.octoapp.base.gcode.render

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.minus
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.data.models.GcodePreviewSettings
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max
import kotlin.system.measureTimeMillis

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 10f
private const val DOUBLE_TAP_ZOOM = 5f
private const val ASYNC_RENDER_RECOMMENDED_THRESHOLD_MS = 20

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
    var useAsyncRender = false
        private set
    private var asyncRenderCache: Bitmap? = null
    private var asyncRenderResult: Bitmap? = null
    var asyncRenderRecommended = false
        private set
    private var asyncRenderBusy = false
    private var pendingAsyncRender = false
    private var renderScope: CoroutineScope? = null
    var isAcceptTouchInput = true
    private var printBed: Drawable? = null
    var renderParams: RenderParams? = null
        set(value) {
            printBed = value?.let {
                ContextCompat.getDrawable(context, it.renderStyle.background)
            }
            renderParams?.renderStyle?.let { rs ->
                rs.remainingLayerPaint.prepare()
                rs.previousLayerPaint.prepare()
                rs.printHeadPaint.prepare()
                Move.Type.values().forEach {
                    rs.paintPalette(it).prepare()
                }
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
        if (isAcceptTouchInput) {

            // Play nice with control center overlay. This view gets touch priority
            when (event.action) {
                MotionEvent.ACTION_DOWN -> parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> parent.requestDisallowInterceptTouchEvent(false)
            }

            scaleGestureDetector.onTouchEvent(event)
            if (!scaleGestureDetector.isInProgress) {
                gestureDetector.onTouchEvent(event)
            }
        }
        return true
    }

    fun enableAsyncRender(coroutineScope: CoroutineScope) {
        renderScope = coroutineScope
        useAsyncRender = true
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
        }.start()
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

    override fun invalidate() {
        if (useAsyncRender) {
            renderAsync()
        } else {
            super.invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) = measureTimeMillis {
        super.onDraw(canvas)

        // Skip draw without params
        val params = renderParams ?: return

        // Enforce scroll limits
        enforceScrollLimits(params)

        // Check HW acceleration
        if (!canvas.isHardwareAccelerated && BuildConfig.DEBUG) {
            Timber.w("Missing hardware acceleration!")
        }

        if (useAsyncRender) {
            asyncRenderResult?.let {
                canvas.drawBitmap(it, 0f, 0f, null)
            }
        } else {
            render(canvas)
        }
    }.let {
        if (BuildConfig.DEBUG) {
            // Do not log in non-debug builds to increase performance
            Timber.v("Draw took ${it}ms")
        }

        if (it > ASYNC_RENDER_RECOMMENDED_THRESHOLD_MS) {
            asyncRenderRecommended = true
        }
    }

    private fun renderAsync(): Unit = measureTimeMillis {
        if (width == 0 || height == 0 || asyncRenderBusy) {
            // Trigger render again after we are done
            pendingAsyncRender = true
            return
        }
        asyncRenderBusy = true

        renderScope?.launch(Dispatchers.Default) {
            // Create bitmap
            val bitmap = asyncRenderCache?.takeIf { it.height == height && it.width == width }
                ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            asyncRenderCache = bitmap


            // Render. This bitmap may be blank when we need to draw to the view.
            bitmap.eraseColor(Color.TRANSPARENT)
            bitmap.applyCanvas {
                render(this)
            }

            // We are done. Paint to view
            post {
                // Flush. This bitmap is always ready to be drawn.
                val result = asyncRenderResult?.takeIf { it.height == height && it.width == width }
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                asyncRenderResult = result
                result.eraseColor(Color.TRANSPARENT)
                result.applyCanvas {
                    drawBitmap(bitmap, 0f, 0f, null)
                }

                // Invalidate to draw to view
                super.invalidate()

                // New data came in while we were busy? Trigger next round
                asyncRenderBusy = false
                if (pendingAsyncRender) {
                    pendingAsyncRender = false
                    renderAsync()
                }
            }
        } ?: run {
            asyncRenderBusy = false
        }
    }.let {
        if (BuildConfig.DEBUG) {
            // Do not log in non-debug builds to increase performance
            Timber.v("Async render took ${it}ms")
        }
    }

    private fun Paint.prepare(): Paint {
        val quality = renderParams?.quality ?: GcodePreviewSettings.Quality.Medium
        isAntiAlias = quality >= GcodePreviewSettings.Quality.Ultra
        strokeCap = if (quality >= GcodePreviewSettings.Quality.Medium) Paint.Cap.ROUND else Paint.Cap.BUTT
        return this
    }

    private fun render(canvas: Canvas) {
        // Skip draw without params
        val params = renderParams ?: return
        val style = params.renderStyle

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
        Move.Type.values().forEach { style.paintPalette(it).strokeWidth = strokeWidth * 0.5f }
        style.paintPalette(Move.Type.Extrude).strokeWidth = strokeWidth
        style.previousLayerPaint.strokeWidth = strokeWidth
        style.remainingLayerPaint.strokeWidth = strokeWidth

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
        if (params.originInCenter) {
            canvas.translate(params.printBedSizeMm.x * 0.5f, params.printBedSizeMm.y * 0.5f)
        }

        // Render previous layer, we do not render travels
        params.renderContext.previousLayerPaths?.filter {
            it.type != Move.Type.Travel
        }?.forEach {
            val paint = style.previousLayerPaint.prepare()
            canvas.drawLines(it.lines, it.linesOffset, it.linesCount, paint)
            it.arcs.forEach { arc ->
                val d = 2 * arc.r
                canvas.drawArc(arc.leftX, arc.topY, arc.leftX + d, arc.topY + d, arc.startAngle, arc.sweepAngle, false, paint)
            }
        }

        // Render completed current layer
        params.renderContext.completedLayerPaths.forEach {
            val paint = style.paintPalette(it.type).prepare()
            canvas.drawLines(it.lines, it.linesOffset, it.linesCount, paint)
            it.arcs.forEach { arc ->
                val d = 2 * arc.r
                canvas.drawArc(arc.leftX, arc.topY, arc.leftX + d, arc.topY + d, arc.startAngle, arc.sweepAngle, false, paint)
            }
        }

        // Render remaining current layer, we do not render travels
        params.renderContext.remainingLayerPaths?.filter {
            it.type != Move.Type.Travel
        }?.forEach {
            val paint = style.remainingLayerPaint.prepare()
            canvas.drawLines(it.lines, it.linesOffset, it.linesCount, paint)
            it.arcs.forEach { arc ->
                val d = 2 * arc.r
                canvas.drawArc(arc.leftX, arc.topY, arc.leftX + d, arc.topY + d, arc.startAngle, arc.sweepAngle, false, paint)
            }
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
                style.printHeadPaint.prepare()
            )
        }
    }

    inner class ScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Check if we increase or decrease zoom
            val newZoom = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
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
                enforceScrollLimits(it)
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
        val originInCenter: Boolean,
        val quality: GcodePreviewSettings.Quality,
    )
}