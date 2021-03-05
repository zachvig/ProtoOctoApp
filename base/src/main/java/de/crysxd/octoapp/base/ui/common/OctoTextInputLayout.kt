package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.view_octo_input_layout.view.*


class OctoTextInputLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {

    private val initialLabelColors: ColorStateList
    var hintNormal: CharSequence? = null
        set(value) {
            field = value
            updateViewState()
        }
    var hintActive: CharSequence? = null
        set(value) {
            field = value
            updateViewState()
        }
    var example: CharSequence? = null
        set(value) {
            field = value
            updateViewState()
        }
    var error: CharSequence? = null
        set(value) {
            field = value

            if (error != null) {
                label.setTextColor(ContextCompat.getColor(context, R.color.color_error))
            } else {
                label.setTextColor(initialLabelColors)
            }

            updateViewState()
        }
    var selectAllOnFocus: Boolean = false
    var actionOnlyWithText: Boolean = false
    private var actionSet = false
    var actionTint: Int?
        set(value) {
            if (value != null) {
                action.setColorFilter(value)
            } else {
                action.clearColorFilter()
            }
        }
        get() = null
    var actionIcon: Int?
        set(value) {
            if (value != null && value > 0) {
                action.setImageResource(value)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (action.drawable as? AnimatedVectorDrawable)?.let {
                        it.start()
                        it.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                            override fun onAnimationEnd(drawable: Drawable) {
                                it.start()
                            }
                        })
                    }
                }
                action.isVisible = true
                actionSet = true
            } else {
                action.setImageDrawable(null)
                action.isVisible = false
                actionSet = false
            }
        }
        get() = null

    @Suppress("unused")
    val editText: AppCompatEditText by lazy { input }

    init {
        View.inflate(context, R.layout.view_octo_input_layout, this)

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateViewState()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OctoTextInputLayout, 0, 0
        ).use {
            hintActive = it.getString(R.styleable.OctoTextInputLayout_labelActive)
            example = it.getString(R.styleable.OctoTextInputLayout_example)
            hintNormal = it.getString(R.styleable.OctoTextInputLayout_label)
            selectAllOnFocus = it.getBoolean(R.styleable.OctoTextInputLayout_selectAllOnFocus, false)
            actionOnlyWithText = it.getBoolean(R.styleable.OctoTextInputLayout_actionOnlyWithText, false)
            input.setText(it.getString(R.styleable.OctoTextInputLayout_defaultInputValue))
            val iconDrawable = it.getResourceId(R.styleable.OctoTextInputLayout_icon, 0)
            if (iconDrawable > 0) {
                icon.setImageResource(iconDrawable)
            } else {
                icon.isVisible = false
            }
            val iconTint = it.getColor(R.styleable.OctoTextInputLayout_iconTint, -1)
            if (iconTint != -1) {
                icon.setColorFilter(iconTint)
            }
            actionIcon = it.getResourceId(R.styleable.OctoTextInputLayout_actionDrawable, 0)
            actionTint = ContextCompat.getColor(context, R.color.accent)
        }

        initialLabelColors = label.textColors

        updateViewState()

        isSaveEnabled = true
        input.isSaveEnabled = false

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && selectAllOnFocus) {
                editText.post {
                    editText.setSelection(0, editText.length())
                }
            }

            updateViewState()
        }
    }

    private fun updateViewState() {
        val labelVisible = input.hasFocus() || !input.text.isNullOrEmpty() || error != null
        label.text = if (input.hasFocus()) {
            error ?: hintActive ?: hintNormal
        } else {
            error ?: hintNormal
        }
        val hintText = if (labelVisible) {
            example
        } else {
            hintNormal
        }
        val actionVisible = actionSet && (!editText.text.isNullOrBlank() || !actionOnlyWithText)

        // Only animate if changes worth animation are detected
        if (labelVisible != label.isVisible || hintText != input.hint || actionVisible != action.isVisible) {
            // Animation causes glitch in font color on older Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                TransitionManager.beginDelayedTransition(this, InstantAutoTransition())
            }
            label.isVisible = labelVisible
            input.hint = hintText
            action.isVisible = actionVisible
        }
    }

    @Suppress("unused")
    fun setOnActionListener(l: (View) -> Unit) {
        action.setOnClickListener(l)
    }

    override fun onSaveInstanceState(): Parcelable = SavedState(
        super.onSaveInstanceState()!!,
        labelText = label.text?.toString() ?: "",
        value = input.text?.toString() ?: "",
        hint = input.hint?.toString() ?: ""
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        label.text = savedState.labelText
        input.setText(savedState.value)
        input.hint = savedState.hint
    }

    @Parcelize
    private class SavedState(
        val parcelable: Parcelable,
        val labelText: String?,
        val value: String?,
        val hint: String?,
    ) : BaseSavedState(parcelable)
}