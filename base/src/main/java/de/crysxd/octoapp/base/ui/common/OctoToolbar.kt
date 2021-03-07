package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ViewOctoToolbarBinding
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition

class OctoToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {
    private val binding = ViewOctoToolbarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.toolbarChips.layoutParams = (binding.toolbarChips.layoutParams as FrameLayout.LayoutParams).also {
            it.gravity = Gravity.CENTER
            it.width = ViewGroup.LayoutParams.WRAP_CONTENT
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

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
        val lightColor = ColorTheme.activeColorTheme.light
        val darkColor = ColorTheme.activeColorTheme.dark

        if (state == State.Hidden) {
            animate().alpha(0f).start()
            return
        }

        if (alpha > 0) {
            TransitionManager.beginDelayedTransition(this, InstantAutoTransition())
        }

        animate().alpha(1f).start()
        binding.textViewStep1Label.isVisible = false
        binding.textViewStep1Label.background?.setTint(lightColor)
        binding.textViewStep2Label.isVisible = false
        binding.textViewStep2Label.background?.setTint(lightColor)
        binding.textViewStep3Label.isVisible = false
        binding.textViewStep3Label.background?.setTint(lightColor)
        binding.viewConnector1To2.background?.setTint(lightColor)
        binding.viewConnector2To3.background?.setTint(lightColor)
        binding.textViewStep1.text = ""
        binding.textViewStep2.text = ""
        binding.textViewStep3.text = ""
        binding.textViewStep1.foregroundCompat?.alpha = 255
        binding.textViewStep2.foregroundCompat?.alpha = 255
        binding.textViewStep3.foregroundCompat?.alpha = 255
        binding.textViewStep1.background.setTint(darkColor)
        binding.textViewStep2.background.setTint(darkColor)
        binding.textViewStep3.background.setTint(darkColor)

        when (state) {
            State.Connect -> {
                binding.textViewStep1Label.isVisible = true
                binding.textViewStep1.text = "1"
                binding.textViewStep1.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.textViewStep1.foregroundCompat?.alpha = 0
            }
            State.Prepare -> {
                binding.textViewStep2Label.isVisible = true
                binding.textViewStep2.text = "2"
                binding.textViewStep2.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.textViewStep2.foregroundCompat?.alpha = 0
            }
            State.Print -> {
                binding.textViewStep3Label.isVisible = true
                binding.textViewStep3.text = "3"
                binding.textViewStep3.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                binding.textViewStep3.foregroundCompat?.alpha = 0
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
