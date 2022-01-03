package de.crysxd.baseui.common

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.transition.TransitionManager
import de.crysxd.baseui.R
import de.crysxd.baseui.utils.InstantAutoTransition

class BottomToolbarSwipeButtonView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    private val swipeAreaPaint = Paint().also {
        it.color = ContextCompat.getColor(context, R.color.input_background)
        it.style = Paint.Style.FILL
    }
    private val swipeButtonGhostPaint = Paint().also {
        it.color = ContextCompat.getColor(context, R.color.accent)
        it.style = Paint.Style.FILL
        it.alpha = 40
    }
    private val swipeButtonPaint = Paint().also {
        it.color = ContextCompat.getColor(context, R.color.accent)
        it.style = Paint.Style.FILL_AND_STROKE
    }
    private val swipeButtonBorderPaint = Paint().also {
        it.color = ContextCompat.getColor(context, R.color.snackbar_positive)
        it.style = Paint.Style.STROKE
        it.alpha = 128
    }
    private val labelPaint = Paint().also {
        val tv = TextView(context)
        TextViewCompat.setTextAppearance(tv, R.style.OctoTheme_TextAppearance_Button)
        it.color = ContextCompat.getColor(context, R.color.accent)
        it.style = Paint.Style.FILL
        it.typeface = tv.typeface
        it.textSize = tv.textSize
    }
    private val swipeButtonPadding = resources.getDimension(R.dimen.margin_0_1)
    private val swipeAreaCornerRadius = resources.getDimension(R.dimen.common_corner_radius)
    private val swipeButtonCornerRadius = resources.getDimension(R.dimen.common_corner_radius) - swipeButtonPadding / 2
    private val swipeAreaRect = RectF()
    private val swipeButtonRect = RectF()
    private val swipeButtonGhostRect = RectF()
    private var swipeButtonIcon: Drawable? = null
    private var swipeButtonLabel: String? = null
    private var swipeButtonConfirmedIcon = ContextCompat.getDrawable(context, R.drawable.ic_round_check_24)
    private var swipeButtonIconTint: Int = ContextCompat.getColor(context, R.color.text_colored_background)
    private var swipeButtonShownAt = 0L
    private val stopSwipeRunnable = Runnable(::stopSwipeButton)
    private var swipeButtonResetAnimator: Animator? = null
    private var confirmedAnimator: Animator? = null
    private var confirmedAction: () -> Unit = {}

    init {
        setWillNotDraw(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(false)
                stopSwipeButton()
            }

            MotionEvent.ACTION_MOVE -> doOnLayout {
                parent.requestDisallowInterceptTouchEvent(true)
                val timeOk = System.currentTimeMillis() > (swipeButtonShownAt + 200)
                val distanceOk = event.x > swipeAreaRect.left + swipeAreaRect.width() * 0.05f
                val wasConfirmed = isConfirmed()

                if (timeOk || distanceOk) {
                    swipeButtonResetAnimator?.cancel()
                    removeCallbacks(stopSwipeRunnable)
                    val buttonWidth = swipeButtonRect.width()
                    swipeButtonRect.left = event.x.coerceIn(getSwipeButtonStartX(), getSwipeButtonEndX())
                    swipeButtonRect.right = swipeButtonRect.left + buttonWidth
                    invalidate()
                }

                if (!wasConfirmed && isConfirmed()) {
                    performHapticConfirmFeedback()
                }

                if (wasConfirmed != isConfirmed()) {
                    animateConfirmed(reversed = !isConfirmed())
                }
            }

            else -> removeCallbacks(stopSwipeRunnable)
        }

        return true
    }

    private fun animateGhost() = doOnLayout {
        ValueAnimator.ofFloat(swipeButtonGhostRect.left, getSwipeButtonEndX()).also {
            it.addUpdateListener { _ ->
                swipeButtonGhostRect.left = it.animatedValue as Float
                swipeButtonGhostRect.right = swipeButtonGhostRect.left + swipeButtonRect.width()
                invalidate()
            }
            it.startDelay = 300
            it.duration = 500
            it.interpolator = DecelerateInterpolator()
        }.start()
    }

    private fun animateConfirmed(reversed: Boolean) = doOnLayout {
        val (start, end) = if (reversed) {
            swipeButtonBorderPaint.strokeWidth to 0f
        } else {
            swipeButtonBorderPaint.strokeWidth to swipeButtonPadding * 2
        }

        confirmedAnimator?.cancel()
        confirmedAnimator = ValueAnimator.ofFloat(start, end).also {
            it.addUpdateListener { _ ->
                swipeButtonBorderPaint.strokeWidth = (it.animatedValue as Float)
                swipeButtonPaint.strokeWidth = swipeButtonBorderPaint.strokeWidth / 3
                invalidate()
            }
            it.duration = 250
            it.interpolator = DecelerateInterpolator()
        }
        confirmedAnimator?.start()
    }

    private fun animateReset() = doOnLayout {
        swipeButtonResetAnimator?.cancel()
        swipeButtonResetAnimator = ValueAnimator.ofFloat(swipeButtonRect.left, getSwipeButtonStartX()).also {
            it.addUpdateListener { _ ->
                val width = swipeButtonRect.width()
                swipeButtonRect.left = it.animatedValue as Float
                swipeButtonRect.right = swipeButtonRect.left + width
                invalidate()
            }
            it.duration = 300
            it.interpolator = DecelerateInterpolator()
        }
        swipeButtonResetAnimator?.start()
    }

    private fun getSwipeButtonStartX() = swipeAreaRect.left + swipeButtonPadding

    private fun getSwipeButtonEndX() = swipeAreaRect.right - swipeButtonPadding - swipeButtonRect.width()

    private fun getSwipeButtonProgress() = (swipeButtonRect.left - getSwipeButtonStartX()) / (getSwipeButtonEndX() * 0.8f - getSwipeButtonStartX())

    private fun isConfirmed() = getSwipeButtonProgress() >= 1f

    fun startSwipeButton(@DrawableRes icon: Int, label: String, button: View, action: () -> Unit) {
        triggerSwipeButtonAnimation()
        isVisible = true
        swipeButtonShownAt = System.currentTimeMillis()
        removeCallbacks(stopSwipeRunnable)
        confirmedAction = action
        swipeButtonLabel = label

        // Get icon
        swipeButtonIcon = ContextCompat.getDrawable(context, icon)

        // Reset paint
        swipeButtonPaint.strokeWidth = 0f
        swipeButtonBorderPaint.strokeWidth = 0f

        invalidate()
        doOnPreDraw {
            // Update swipe area/button position
            swipeAreaRect.left = button.x
            swipeButtonRect.left = swipeAreaRect.left + swipeButtonPadding
            swipeButtonRect.right = swipeButtonRect.left + button.width

            // Reset ghost
            swipeButtonGhostRect.left = swipeButtonRect.left
            swipeButtonGhostRect.top = swipeButtonRect.top
            swipeButtonGhostRect.right = swipeButtonRect.right
            swipeButtonGhostRect.bottom = swipeButtonRect.bottom

            animateGhost()
            performHapticStartFeedback()
        }
    }


    private fun performHapticConfirmFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    private fun performHapticRejectFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    private fun performHapticStartFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun stopSwipeButton() {
        if (isConfirmed() || (System.currentTimeMillis() - swipeButtonShownAt) > 500) {
            triggerSwipeButtonAnimation()
            isVisible = false
            removeCallbacks(stopSwipeRunnable)
            if (!isConfirmed()) {
                performHapticRejectFeedback()
            } else {
                confirmedAction()
            }
        } else {
            animateReset()
            postDelayed(stopSwipeRunnable, 1000)
        }
    }

    private fun triggerSwipeButtonAnimation() {
        val parent = parent as ViewGroup
        TransitionManager.beginDelayedTransition(
            parent,
            InstantAutoTransition(
                quickTransition = false,
                explode = true,
                explodeEpicenter = Rect(0, 0, parent.width, 0)
            ).also { transition ->
                transition.duration = 150
                parent.children.filter { it.id != R.id.swipeButtons }.forEach {
                    transition.excludeTarget(it, true)
                }
            }
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        swipeAreaRect.top = resources.getDimension(R.dimen.margin_1)// * 2 + labelPaint.textSize
        swipeAreaRect.right = w - resources.getDimension(R.dimen.margin_1)
        swipeAreaRect.bottom = h - resources.getDimension(R.dimen.margin_1)
        swipeButtonRect.top = swipeAreaRect.top + swipeButtonPadding
        swipeButtonRect.bottom = swipeAreaRect.bottom - swipeButtonPadding
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Ghost can never be behind the actual button
        swipeButtonGhostRect.left = swipeButtonGhostRect.left.coerceAtLeast(swipeButtonRect.left)
        swipeButtonGhostRect.right = swipeButtonGhostRect.left + swipeButtonRect.width()

        // Area
        canvas.drawRoundRect(swipeAreaRect, swipeAreaCornerRadius, swipeAreaCornerRadius, swipeAreaPaint)

        // Label
        swipeButtonLabel?.let {
            labelPaint.alpha = (255 - getSwipeButtonProgress() * 2f * 255).toInt().coerceIn(0, 255)
            canvas.drawText(
                it,
                swipeAreaRect.centerX() - labelPaint.measureText(it) / 2,
                swipeAreaRect.centerY() + labelPaint.textSize / 2,
                labelPaint
            )
        }

        // Ghost
        canvas.drawRoundRect(swipeButtonGhostRect, swipeButtonCornerRadius, swipeButtonCornerRadius, swipeButtonGhostPaint)
        swipeButtonIcon?.alpha = swipeButtonGhostPaint.alpha
        swipeButtonIcon.drawOnSwipeButton(swipeButtonGhostRect, canvas)

        // Button
        canvas.drawRoundRect(swipeButtonRect, swipeButtonCornerRadius, swipeButtonCornerRadius, swipeButtonBorderPaint)
        canvas.drawRoundRect(swipeButtonRect, swipeButtonCornerRadius, swipeButtonCornerRadius, swipeButtonPaint)
        swipeButtonIcon?.alpha = swipeButtonPaint.alpha
        if (isConfirmed()) {
            swipeButtonConfirmedIcon.drawOnSwipeButton(swipeButtonRect, canvas)
        } else {
            swipeButtonIcon.drawOnSwipeButton(swipeButtonRect, canvas)
        }
    }

    private fun Drawable?.drawOnSwipeButton(buttonRect: RectF, canvas: Canvas) = this?.let {
        // Same distance from top as horizontal
        val left = buttonRect.centerX() - it.intrinsicWidth / 2
        val top = buttonRect.top + (left - buttonRect.left)
        it.setBounds(
            left.toInt(),
            top.toInt(),
            (buttonRect.centerX() + it.intrinsicWidth / 2).toInt(),
            (top + it.intrinsicHeight).toInt(),
        )
        it.setTint(swipeButtonIconTint)
        it.draw(canvas)
    }
}