package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.animation.InstantAutoTransition
import kotlinx.android.synthetic.main.view_octo_input_layout.view.*

class OctoTextInputLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {

    private val initialLabelColors: ColorStateList
    private val initialLabelText: CharSequence

    init {
        View.inflate(context, R.layout.view_octo_input_layout, this)

        input.setOnFocusChangeListener { _, _ ->
            updateViewState()
        }

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OctoTextInputLayout, 0, 0
        ).use {
            label.text = it.getString(R.styleable.OctoTextInputLayout_label)
            input.setText(it.getString(R.styleable.OctoTextInputLayout_defaultInputValue))
        }

        initialLabelColors = label.textColors
        initialLabelText = label.text

        updateViewState()
    }

    private fun updateViewState() {
        TransitionManager.beginDelayedTransition(this, InstantAutoTransition())

        label.isVisible = input.hasFocus() || !input.text.isNullOrEmpty() || label.text != initialLabelText
        input.hint = if (label.isVisible) {
            ""
        } else {
            initialLabelText
        }
    }

    @Suppress("unused")
    fun setError(error: CharSequence?) {
        if (error != null) {
            label.setTextColor(ContextCompat.getColor(context, R.color.color_error))
            label.text = error
        } else {
            label.setTextColor(initialLabelColors)
            label.text = initialLabelText
        }

        updateViewState()
    }

    @Suppress("unused")
    val editText: AppCompatEditText = input
}