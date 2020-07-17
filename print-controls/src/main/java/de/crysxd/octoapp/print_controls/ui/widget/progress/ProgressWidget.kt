package de.crysxd.octoapp.print_controls.ui.widget.progress

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_progress.view.*

class ProgressWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: ProgressWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = context.getString(R.string.progress)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_progress, container, false)

    override fun onViewCreated(view: View) {
        viewModel.printState.observe(viewLifecycleOwner, Observer {
            TransitionManager.beginDelayedTransition(view as ViewGroup)

            val progressPercent = it.progress?.completion ?: 0f
            val progressPercentLayoutThreshold = 80f

            ConstraintSet().apply {
                clone(view as ConstraintLayout)
                constrainPercentWidth(R.id.progressBar, progressPercent / 100f)
                centerVertically(R.id.textViewProgressPercent, R.id.progressBar)
                clear(R.id.textViewProgressPercent, ConstraintSet.END)
                clear(R.id.textViewProgressPercent, ConstraintSet.START)
                connect(
                    R.id.textViewProgressPercent,
                    if (progressPercent > progressPercentLayoutThreshold) {
                        ConstraintSet.END
                    } else {
                        ConstraintSet.START
                    },
                    R.id.progressBar,
                    ConstraintSet.END
                )
            }.applyTo(view as ConstraintLayout)

            view.textViewProgressPercent.setTextColor(
                ContextCompat.getColor(
                    requireContext(), if (progressPercent > progressPercentLayoutThreshold) {
                        R.color.inverse_text
                    } else {
                        R.color.normal_text
                    }
                )
            )

            view.textViewProgressPercent.text = requireContext().getString(R.string.x_percent, progressPercent)
            view.textViewTimeSpent.text = it.progress?.printTime?.let(::formatDuration)
            view.textViewTimeLeft.text = it.progress?.printTimeLeft?.let(::formatDuration)
            view.textViewEstimation.text = it.progress?.printTimeLeftOrigin
        })
    }

    private fun formatDuration(seconds: Int): String = if (seconds < 60) {
        seconds.toString()
    } else {
        DateUtils.formatElapsedTime(seconds.toLong())
    }
}