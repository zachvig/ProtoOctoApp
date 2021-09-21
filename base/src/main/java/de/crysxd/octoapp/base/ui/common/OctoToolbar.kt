package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.OctoToolbarBinding
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.ui.utils.ColorTheme
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition

class OctoToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {
    private val binding = OctoToolbarBinding.inflate(LayoutInflater.from(context), this)

    init {
        alpha = 0f
    }

    var state: State = State.Hidden
        set(value) {
            if (field == value) {
                return
            }

            field = value
            bindState()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ColorTheme.notifyAboutColorChangesUntilDetachedFromWindow(this) {
            bindState()
        }
    }

    private fun bindState() {
        val lightColor = if (isInEditMode) Color.MAGENTA else ColorTheme.activeColorTheme.light
        val darkColor = if (isInEditMode) Color.MAGENTA else ColorTheme.activeColorTheme.dark

        if (state == State.Hidden) {
            setOnClickListener(null)
            animate().alpha(0f).start()
            return
        } else {
            setOnClickListener {
                OctoActivity.instance?.showDialog(message = context.getString(R.string.workspace___explainer))
            }
        }

        if (alpha > 0) {
            TransitionManager.beginDelayedTransition(this, InstantAutoTransition())
        }

        animate().alpha(1f).start()
        binding.chips.textViewStep1Label.isVisible = false
        binding.chips.textViewStep1Label.background?.setTint(lightColor)
        binding.chips.textViewStep2Label.isVisible = false
        binding.chips.textViewStep2Label.background?.setTint(lightColor)
        binding.chips.textViewStep3Label.isVisible = false
        binding.chips.textViewStep3Label.background?.setTint(lightColor)
        binding.chips.viewConnector1To2.background?.setTint(lightColor)
        binding.chips.viewConnector2To3.background?.setTint(lightColor)
        binding.chips.textViewStep1.text = ""
        binding.chips.textViewStep2.text = ""
        binding.chips.textViewStep3.text = ""
        binding.chips.textViewStep1.foregroundTintListCompat = binding.chips.textViewStep1.textColors
        binding.chips.textViewStep2.foregroundTintListCompat = binding.chips.textViewStep2.textColors
        binding.chips.textViewStep3.foregroundTintListCompat = binding.chips.textViewStep3.textColors
        binding.chips.textViewStep1.background.setTint(darkColor)
        binding.chips.textViewStep2.background.setTint(darkColor)
        binding.chips.textViewStep3.background.setTint(darkColor)

        when (state) {
            State.Connect -> {
                binding.chips.textViewStep1Label.isVisible = true
                binding.chips.textViewStep1.text = "1"
                binding.chips.textViewStep1.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.chips.textViewStep1.foregroundTintListCompat = ColorStateList.valueOf(Color.TRANSPARENT)
            }
            State.Prepare -> {
                binding.chips.textViewStep2Label.isVisible = true
                binding.chips.textViewStep2.text = "2"
                binding.chips.textViewStep2.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.chips.textViewStep2.foregroundTintListCompat = ColorStateList.valueOf(Color.TRANSPARENT)
            }
            State.Print -> {
                binding.chips.textViewStep3Label.isVisible = true
                binding.chips.textViewStep3.text = "3"
                binding.chips.textViewStep3.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.chips.textViewStep3.foregroundTintListCompat = ColorStateList.valueOf(Color.TRANSPARENT)
            }
            State.Hidden -> Unit
        }
    }

    private var View.foregroundTintListCompat
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foregroundTintList
        } else {
            null
        }
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foregroundTintList = value
            }
        }

    sealed class State {
        object Connect : State()
        object Prepare : State()
        object Print : State()
        object Hidden : State()
    }
}
