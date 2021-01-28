package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.android.synthetic.main.view_octo_toolbar.view.*

class OctoToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {

    init {
        View.inflate(context, R.layout.view_octo_toolbar, this)
        toolbarChips.layoutParams = (toolbarChips.layoutParams as FrameLayout.LayoutParams).also {
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
        textViewStep1Label.isVisible = false
        textViewStep1Label.background?.setTint(lightColor)
        textViewStep2Label.isVisible = false
        textViewStep2Label.background?.setTint(lightColor)
        textViewStep3Label.isVisible = false
        textViewStep3Label.background?.setTint(lightColor)
        viewConnector1To2.background?.setTint(lightColor)
        viewConnector2To3.background?.setTint(lightColor)
        textViewStep1.text = ""
        textViewStep2.text = ""
        textViewStep3.text = ""
        textViewStep1.foregroundCompat?.alpha = 255
        textViewStep2.foregroundCompat?.alpha = 255
        textViewStep3.foregroundCompat?.alpha = 255
        textViewStep1.background.setTint(darkColor)
        textViewStep2.background.setTint(darkColor)
        textViewStep3.background.setTint(darkColor)

        when (state) {
            State.Connect -> {
                textViewStep1Label.isVisible = true
                textViewStep1.text = "1"
                textViewStep1.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                textViewStep1.foregroundCompat?.alpha = 0
            }
            State.Prepare -> {
                textViewStep2Label.isVisible = true
                textViewStep2.text = "2"
                textViewStep2.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                textViewStep2.foregroundCompat?.alpha = 0
            }
            State.Print -> {
                textViewStep3Label.isVisible = true
                textViewStep3.text = "3"
                textViewStep3.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
                textViewStep3.foregroundCompat?.alpha = 0
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
