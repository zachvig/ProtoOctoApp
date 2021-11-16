package de.crysxd.baseui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.core.graphics.transform
import androidx.core.view.children
import timber.log.Timber
import kotlin.math.min

class MatrixView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) : ViewGroup(context, attributeSet) {

    private var minZoom = 1f
    private val maxZoom get() = minZoom * 10f
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())
    private var currentZoom = minZoom
    private val imageRect = RectF()
    private val viewPortRect = RectF()
    private val helperRect = RectF()
    private val helperMatrix = Matrix()
    var matrixInput: MatrixInput? = null
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    private val debugPaint = Paint().also {
        it.strokeWidth = 20f
        it.style = Paint.Style.STROKE
    }

    init {
        setWillNotDraw(false)
    }


    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        debugPaint.color = Color.RED
        canvas.drawRect(viewPortRect, debugPaint)
        debugPaint.color = Color.GREEN
        canvas.drawRect(imageRect, debugPaint)
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewPortRect.top = 0f
        viewPortRect.left = 0f
        viewPortRect.right = w.toFloat()
        viewPortRect.bottom = h.toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        children.forEach {
            it.measure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val input = matrixInput ?: return children.forEach {
            it.layout(l, t, r, b)
        }

        val w = r - l
        val h = b - t

        // Get content dimensions (rotated) and determine min zoom which is required
        // to show the entire content in the view bounds
        val (cw, ch) = if (input.rotate90) {
            input.contentHeight to input.contentWidth
        } else {
            input.contentWidth to input.contentHeight
        }

        // Calculate scale to fit content and view bounds
        minZoom = min(w.toFloat() / cw, h.toFloat() / ch)
        currentZoom = minZoom
        val rotation = if (input.rotate90) 90f else 0f
        val vw = input.contentWidth
        val vh = input.contentHeight
        val vx = w / 2 - vw / 2
        val vy = h / 2 - vh / 2

        // Update image rect
        imageRect.top = vy.toFloat()
        imageRect.bottom = (vy + vh).toFloat()
        imageRect.left = vx.toFloat()
        imageRect.right = (vx + vw).toFloat()
        helperMatrix.reset()
        helperMatrix.postRotate(rotation, imageRect.centerX(), imageRect.centerY())
        imageRect.scale(minZoom, imageRect.centerX(), imageRect.centerY()).transform(helperMatrix)

        Timber.i("contentSize=${input.contentWidth}x${input.contentHeight}px viewSize=${vw}x${vh}px scale=$minZoom")
        children.forEach {
            // Layout
            it.layout(vx, vy, vx + vw, vy + vh)
            Timber.i("view=$it")

            // Rotate and flip views
            it.scaleX = minZoom * input.scaleXMultiplier
            it.scaleY = minZoom * input.scaleYMultiplier
            it.rotation = rotation
        }
    }

    inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            if (matrixInput?.scaleToFill == true) {
//                matrixInput = matrixInput?.copy(scaleToFill = false)
//            }

            // Limit the scale factor so we never go below min scale
            val scaleFactorMin = minZoom / currentZoom
            val scaleFactorMax = maxZoom / currentZoom
            val scaleFactor = detector.scaleFactor.coerceIn(scaleFactorMin, scaleFactorMax)
            currentZoom *= scaleFactor
            imageRect.scale(scaleFactor, detector.focusX, detector.focusY)
                .limitBounds(viewPortRect).flushToViews()
            return true
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) = Unit

        override fun onSingleTapUp(e: MotionEvent) = false

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            imageRect.translate(distanceX, distanceY).limitBounds(viewPortRect).flushToViews()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent?) = Unit

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) = false

    }

    private fun RectF.scale(scale: Float, focusX: Float, focusY: Float) = transform(Matrix().also {
        it.postScale(scale, scale, focusX, focusY)
    })

    private fun RectF.translate(x: Float, y: Float) = transform(Matrix().also {
        it.postTranslate(-x, -y)
    })

    private fun RectF.flushToViews() = children.forEach { view ->
        val input = matrixInput ?: return@forEach
        helperRect.copyFrom(this)
        val scale = width() / view.width
        val inverseScale = 1 / scale
        helperMatrix.reset()
        helperMatrix.preScale(inverseScale, inverseScale, helperRect.centerX(), helperRect.centerY())
        helperMatrix.postRotate(if (input.rotate90) -90f else 0f, helperRect.centerX(), helperRect.centerY())
        helperRect.transform(helperMatrix)

        view.scaleX = scale * input.scaleXMultiplier
        view.scaleY = scale * input.scaleYMultiplier
        view.translationX = helperRect.left - view.left
        view.translationY = helperRect.top - view.top
    }

    private fun RectF.copyFrom(other: RectF) {
        left = other.left
        top = other.top
        right = other.right
        bottom = other.bottom
    }

    private fun RectF.limitBounds(bounds: RectF) = transform(Matrix().also {
        var dx = 0f
        var dy = 0f
        val input = matrixInput ?: return@also

        // Limit left right movement
        if (bounds.width() > width()) {
            dx = bounds.centerX() - centerX()
        } else if (left > bounds.left) {
            dx = bounds.left - left
        } else if (right < bounds.right) {
            dx = bounds.right - right
        }

        // Limit top bottom movement
        if (bounds.height() > height()) {
            dy = bounds.centerY() - centerY()
        } else if (top > bounds.top) {
            dy = bounds.top - top
        } else if (bottom < bounds.bottom) {
            dy = bounds.bottom - bottom
        }

        it.postTranslate(dx, dy)
    })

    data class MatrixInput(
        val scaleToFill: Boolean,
        val flipH: Boolean,
        val flipV: Boolean,
        val rotate90: Boolean,
        val contentWidth: Int,
        val contentHeight: Int,
    ) {
        val scaleXMultiplier get() = if (flipH) -1f else 1f
        val scaleYMultiplier get() = if (flipV) -1f else 1f
    }
}
