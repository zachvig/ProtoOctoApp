package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
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

        setActiveStep(Step.Connect)
    }

    @SuppressWarnings("unused")
    fun setActiveStep(step: Step) {
        TransitionManager.beginDelayedTransition(this, InstantAutoTransition())

        textViewStep1Label.isVisible = false
        textViewStep2Label.isVisible = false
        textViewStep3Label.isVisible = false
        textViewStep1.text = ""
        textViewStep2.text = ""
        textViewStep3.text = ""
        textViewStep1.setBackgroundResource(R.drawable.bg_toolbar_chip_number_hidden)
        textViewStep2.setBackgroundResource(R.drawable.bg_toolbar_chip_number_hidden)
        textViewStep3.setBackgroundResource(R.drawable.bg_toolbar_chip_number_hidden)

        when (step) {
            Step.Connect -> {
                textViewStep1Label.isVisible = true
                textViewStep1.text = "1"
                textViewStep1.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
            }
            Step.Prepare -> {
                textViewStep2Label.isVisible = true
                textViewStep2.text = "2"
                textViewStep2.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
            }
            Step.Print -> {
                textViewStep3Label.isVisible = true
                textViewStep3.text = "3"
                textViewStep3.setBackgroundResource(R.drawable.bg_toolbar_chip_number)
            }
        }
    }

    sealed class Step {
        object Connect : Step()
        object Prepare : Step()
        object Print : Step()
    }
}
