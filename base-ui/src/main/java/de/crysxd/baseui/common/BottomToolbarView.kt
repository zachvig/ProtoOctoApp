package de.crysxd.baseui.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.updatePadding
import androidx.transition.TransitionManager
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.BottomToolbarViewBinding
import de.crysxd.baseui.utils.InstantAutoTransition

class BottomToolbarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    private val binding = BottomToolbarViewBinding.inflate(LayoutInflater.from(context), this)
    private val initialFlowViewCount = binding.flow.referencedIds.size
    val menuButton get() = binding.menu

    init {
        elevation = resources.getDimension(R.dimen.margin_1)
        updatePadding(
            top = resources.getDimensionPixelSize(R.dimen.margin_0_1),
            bottom = resources.getDimensionPixelSize(R.dimen.margin_0_1)
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    fun addAction(@DrawableRes icon: Int, @StringRes title: Int, needsSwipe: Boolean, action: () -> Unit): List<View> {
        val group = mutableListOf<View>()

        if (binding.flow.referencedIds.size > initialFlowViewCount) {
            val separator = View(context)
            separator.id = ViewCompat.generateViewId()
            separator.setBackgroundResource(R.color.input_background)
            addView(
                separator,
                ViewGroup.LayoutParams(
                    resources.getDimension(R.dimen.bottom_toolbar_separator_width).toInt(),
                    resources.getDimension(R.dimen.bottom_toolbar_separator_height).toInt(),
                )
            )
            binding.flow.addView(separator)
            group.add(separator)
        }

        val button = AppCompatImageButton(ContextThemeWrapper(context, R.style.OctoTheme_Widget_Button_Image))
        button.setImageResource(icon)
        button.id = ViewCompat.generateViewId()
        button.contentDescription = context.getString(title)
        addView(button)
        binding.flow.addView(button)
        group.add(button)

        if (needsSwipe) {
            button.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> requireSwipeButtons().startSwipeButton(
                        icon = icon,
                        label = context.getString(title),
                        button = button,
                        action = action
                    )
                    else -> {
                        event.setLocation(event.x + button.x, event.y)
                        requireSwipeButtons().onTouchEvent(event)
                    }
                }

                true
            }
        } else {
            button.setOnClickListener {
                action()
            }
        }

        return group
    }

    fun setStatus(status: CharSequence?) {
        binding.status.text = status
        binding.status.isVisible = !status.isNullOrBlank()
    }

    fun startDelayedTransition() = TransitionManager.beginDelayedTransition(this, InstantAutoTransition(changeBounds = false, fadeText = true))

    fun setMainAction(@StringRes text: Int): Button {
        binding.main.setText(text)
        binding.main.isVisible = true
        return binding.main
    }

    private fun requireSwipeButtons() = (parent as ViewGroup).children.firstOrNull { it.id == R.id.swipeButtons } as? BottomToolbarSwipeButtonView
        ?: throw IllegalStateException("No swipe buttons")
}
