package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.OctoToolbarBinding
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition

class OctoToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {
    private val binding = OctoToolbarBinding.inflate(LayoutInflater.from(context), this)

    init {
        alpha = 0f
    }

    var isCloudIndicatorVisible: Boolean
        set(value) {
            binding.cloudIndicator.isVisible = value
        }
        get() = binding.cloudIndicator.isVisible

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
            animate().alpha(0f).start()
            return
        }

        if (alpha > 0) {
            TransitionManager.beginDelayedTransition(this, InstantAutoTransition())
        }

        animate().alpha(1f).start()
        binding.cloudIndicator.setColorFilter(darkColor)
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
        binding.chips.textViewStep1.foregroundCompat?.alpha = 255
        binding.chips.textViewStep2.foregroundCompat?.alpha = 255
        binding.chips.textViewStep3.foregroundCompat?.alpha = 255
        binding.chips.textViewStep1.background.setTint(darkColor)
        binding.chips.textViewStep2.background.setTint(darkColor)
        binding.chips.textViewStep3.background.setTint(darkColor)

        when (state) {
            State.Connect -> {
                binding.chips.textViewStep1Label.isVisible = true
                binding.chips.textViewStep1.text = "1"
                binding.chips.textViewStep1.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.chips.textViewStep1.foregroundCompat?.alpha = 0
            }
            State.Prepare -> {
                binding.chips.textViewStep2Label.isVisible = true
                binding.chips.textViewStep2.text = "2"
                binding.chips.textViewStep2.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.chips.textViewStep2.foregroundCompat?.alpha = 0
            }
            State.Print -> {
                binding.chips.textViewStep3Label.isVisible = true
                binding.chips.textViewStep3.text = "3"
                binding.chips.textViewStep3.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.chips.textViewStep3.foregroundCompat?.alpha = 0
            }
            State.Hidden -> Unit
        }
    }

    private val View.foregroundCompat
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground
        } else {
            null
        }

    sealed class State {
        object Connect : State()
        object Prepare : State()
        object Print : State()
        object Hidden : State()
    }
}
