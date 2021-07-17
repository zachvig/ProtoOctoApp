package de.crysxd.octoapp.base.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ViewOctoInputLayoutBinding
import de.crysxd.octoapp.base.ui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requestFocusAndOpenSoftKeyboard
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import kotlin.math.roundToInt


class OctoTextInputLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {

    private val binding = ViewOctoInputLayoutBinding.inflate(LayoutInflater.from(context), this, true)

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
    var examples: List<CharSequence> = emptyList()
        set(value) {
            field = value
            visibleExampleIndex = 0
        }
    var visibleExampleIndex = 0
        set(value) {
            field = value
            updateViewState()
        }
    var error: CharSequence? = null
        set(value) {
            field = value

            if (error != null) {
                binding.label.setTextColor(ContextCompat.getColor(context, R.color.color_error))
            } else {
                binding.label.setTextColor(initialLabelColors)
            }

            updateViewState()
        }
    var selectAllOnFocus: Boolean = false
    var actionOnlyWithText: Boolean = false
    private var actionSet = false
    var actionTint: Int?
        set(value) {
            if (value != null) {
                binding.action.setColorFilter(value)
            } else {
                binding.action.clearColorFilter()
            }
        }
        get() = null
    var backgroundTint: Int?
        set(value) {
            if (value != null) {
                binding.root.backgroundTintList = ColorStateList.valueOf(value)
            } else {
                binding.root.backgroundTintList = null
            }
        }
        get() = null
    var actionIcon: Int?
        set(value) {
            if (value != null && value > 0) {
                binding.action.setImageResource(value)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (binding.action.drawable as? AnimatedVectorDrawable)?.let {
                        it.start()
                        it.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                            override fun onAnimationEnd(drawable: Drawable) {
                                it.start()
                            }
                        })
                    }
                }
                binding.action.isVisible = true
                actionSet = true
            } else {
                binding.action.setImageDrawable(null)
                binding.action.isVisible = false
                actionSet = false
            }
        }
        get() = null

    private val changeExampleRunnable = Runnable {
        showNextExample()
    }

    @Suppress("unused")
    val editText: AppCompatEditText by lazy { binding.input }

    init {
        binding.input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateViewState()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OctoTextInputLayout, 0, 0
        ).use {
            hintActive = it.getString(R.styleable.OctoTextInputLayout_labelActive)
            examples = it.getString(R.styleable.OctoTextInputLayout_example)?.split("|") ?: emptyList()
            hintNormal = it.getString(R.styleable.OctoTextInputLayout_label)
            selectAllOnFocus = it.getBoolean(R.styleable.OctoTextInputLayout_selectAllOnFocus, false)
            actionOnlyWithText = it.getBoolean(R.styleable.OctoTextInputLayout_actionOnlyWithText, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.getString(R.styleable.OctoTextInputLayout_android_autofillHints)?.split("|")?.let { hints ->
                    binding.input.setAutofillHints(*hints.toTypedArray())
                    binding.input.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                }
            }
            binding.input.inputType = it.getInt(R.styleable.OctoTextInputLayout_android_inputType, InputType.TYPE_CLASS_TEXT)
            binding.input.setText(it.getString(R.styleable.OctoTextInputLayout_defaultInputValue))
            val iconDrawable = it.getResourceId(R.styleable.OctoTextInputLayout_icon, 0)
            if (iconDrawable > 0) {
                binding.icon.setImageResource(iconDrawable)
            } else {
                binding.icon.isVisible = false
            }
            val iconTint = it.getColor(R.styleable.OctoTextInputLayout_iconTint, -1)
            if (iconTint != -1) {
                binding.icon.setColorFilter(iconTint)
            }
            actionIcon = it.getResourceId(R.styleable.OctoTextInputLayout_actionDrawable, 0)
            actionTint = ContextCompat.getColor(context, R.color.accent)
            if (it.getBoolean(R.styleable.OctoTextInputLayout_forPassword, false)) {
                setPasswordVisible(false)
            }
        }

        initialLabelColors = binding.label.textColors

        updateViewState()

        isSaveEnabled = true
        binding.input.isSaveEnabled = false

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && selectAllOnFocus) {
                editText.post {
                    editText.setSelection(0, editText.length())
                }
            }

            updateViewState()
        }
    }

    fun setPasswordVisible(visible: Boolean) {
        actionIcon = if (visible) R.drawable.ic_round_visibility_off_24 else R.drawable.ic_round_visibility_24
        setOnActionListener { setPasswordVisible(!visible) }
        binding.input.transformationMethod = if (visible) null else PasswordTransformationMethod()
    }

    private fun animateExampleAlpha(from: Float, to: Float, then: () -> Unit = {}) {
        ValueAnimator.ofFloat(from, to).also {
            it.addUpdateListener {
                val color = binding.input.hintTextColors.defaultColor
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val a = ((it.animatedValue as Float) * 255f).roundToInt()
                val updatedColor = Color.argb(a, r, g, b)
                binding.input.setHintTextColor(updatedColor)
            }
            it.doOnEnd {
                then()
            }
            it.duration = 250
            it.interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    private fun showNextExample() {
        // Only show next example when the input has focus as examples are only shown in focused state
        if (binding.input.hasFocus()) {
            animateExampleAlpha(1f, 0f) {
                visibleExampleIndex = (visibleExampleIndex + 1) % examples.size
                Timber.i("visibleExampleIndex=$visibleExampleIndex")
                animateExampleAlpha(0f, 1f)
            }
        }
    }

    private fun scheduleNextExampleToBeShown() {
        // Might be null if called too early
        handler?.removeCallbacks(changeExampleRunnable)
        handler?.postDelayed(changeExampleRunnable, 2500)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleNextExampleToBeShown()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(changeExampleRunnable)
    }

    private fun updateViewState() {
        val labelVisible = binding.input.hasFocus() || !binding.input.text.isNullOrEmpty() || error != null
        binding.label.text = if (binding.input.hasFocus()) {
            error ?: hintActive ?: hintNormal
        } else {
            error ?: hintNormal
        }
        val hintText = if (labelVisible) {
            examples.getOrNull(visibleExampleIndex) ?: examples.lastOrNull()
        } else {
            hintNormal
        }
        val actionVisible = actionSet && (!editText.text.isNullOrBlank() || !actionOnlyWithText)

        // Only animate if changes worth animation are detected
        if (labelVisible != binding.label.isVisible || hintText != binding.input.hint || actionVisible != binding.action.isVisible) {
            // Animation causes glitch in font color on older Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                TransitionManager.beginDelayedTransition(this, InstantAutoTransition())
            }
            binding.label.isVisible = labelVisible
            binding.input.hint = hintText
            binding.action.isVisible = actionVisible
        }

        scheduleNextExampleToBeShown()
    }

    @Suppress("unused")
    fun setOnActionListener(l: (View) -> Unit) {
        binding.action.setOnClickListener(l)
    }

    fun showSoftKeyboard() = binding.input.requestFocusAndOpenSoftKeyboard()
    fun hideSoftKeyboard() = binding.input.clearFocusAndHideSoftKeyboard()

    override fun onSaveInstanceState(): Parcelable = SavedState(
        super.onSaveInstanceState()!!,
        labelText = binding.label.text?.toString() ?: "",
        value = binding.input.text?.toString() ?: "",
        hint = binding.input.hint?.toString() ?: ""
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        binding.label.text = savedState.labelText
        binding.input.setText(savedState.value)
        binding.input.hint = savedState.hint
    }

    @Parcelize
    private class SavedState(
        val parcelable: Parcelable,
        val labelText: String?,
        val value: String?,
        val hint: String?,
    ) : BaseSavedState(parcelable)
}