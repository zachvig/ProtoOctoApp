package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
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
            val actionDrawable = it.getResourceId(R.styleable.OctoTextInputLayout_actionDrawable, 0)
            if (actionDrawable > 0) {
                action.setImageResource(actionDrawable)
            } else {
                action.isVisible = false
            }
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

        // Only animate if changes worth animation are detected
        if (labelVisible != label.isVisible || hintText != input.hint) {
            TransitionManager.beginDelayedTransition(this, InstantAutoTransition())
            label.isVisible = labelVisible
            input.hint = hintText
        }
    }

    @Suppress("unused")
    fun setOnActionListener(l: (View) -> Unit) {
        action.setOnClickListener(l)
    }

    override fun onSaveInstanceState(): Parcelable {
        val savedState = SavedState(super.onSaveInstanceState()!!)
        savedState.labelText = label.text
        savedState.value = input.text.toString()
        savedState.hint = input.hint ?: ""
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        label.text = savedState.labelText
        input.setText(savedState.value)
        input.hint = savedState.hint
    }

    private class SavedState : BaseSavedState {
        lateinit var labelText: CharSequence
        lateinit var value: CharSequence
        lateinit var hint: CharSequence

        constructor(parcelable: Parcelable) : super(parcelable)

        constructor(parcel: Parcel) : super(parcel) {
            labelText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
            value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
            hint = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            TextUtils.writeToParcel(labelText, out, flags)
            TextUtils.writeToParcel(value, out, flags)
            TextUtils.writeToParcel(hint, out, flags)
        }

        @Suppress("unused", "PropertyName")
        val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                return SavedState(`in`)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return Array(size) { null }
            }
        }
    }
}