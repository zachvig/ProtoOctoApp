package de.crysxd.octoapp.print_controls.ui.widget.progress

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.ClipDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_progress.view.*
import kotlin.math.roundToInt

class ProgressWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: ProgressWidgetViewModel by injectViewModel()
    private val formatDurationUseCase: FormatDurationUseCase = Injector.get().formatDurationUseCase()
    private var lastProgress: Float? = null

    override fun getTitle(context: Context) = context.getString(R.string.progress)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_progress, container, false)

    override fun onViewCreated(view: View) {
        viewModel.printState.observe(viewLifecycleOwner, Observer {
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                val progressPercent = it.progress?.completion ?: 0f
                val progressPercentLayoutThreshold = 80f
                val progress = progressPercent.toInt() / 100f
                val progressText = requireContext().getString(R.string.x_percent, progress * 100f)

                if (lastProgress != progress) {
                    TransitionManager.beginDelayedTransition(view as ViewGroup, InstantAutoTransition())
                }

                ConstraintSet().apply {
                    clone(view as ConstraintLayout)
                    constrainPercentWidth(R.id.progressBar, progress)
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
                            R.color.text_colored_background
                        } else {
                            R.color.normal_text
                        }
                    )
                )

                (view.progressBarFill.background as? ClipDrawable)?.apply {
                    ObjectAnimator.ofInt(this, "level", level, (10000f * progress).roundToInt()).start()
                }

                val eta = it.progress?.let { Injector.get().formatEtaUseCase().execute(it.printTimeLeft) }
                view.textViewProgressPercent.text = progressText
                view.textViewPrintName.text = it.job?.file?.display
                view.textViewTimeSpent.text = it.progress?.printTime?.toLong()?.let { formatDuration(it) }
                view.textViewTimeLeft.text = it.progress?.printTimeLeft?.toLong()?.let { formatDuration(it) }
                view.textVieEta.text = requireContext().getString(R.string.x_y, eta, it.progress?.printTimeLeftOrigin)

                view.textViewProgressPercent.isVisible = true
                view.textViewPrintName.isVisible = true
                view.textViewTimeSpent.isVisible = true
                view.textViewTimeLeft.isVisible = true
                view.textVieEta.isVisible = true

                lastProgress = progress
            }
        })
    }

    private suspend fun formatDuration(seconds: Long) = formatDurationUseCase.execute(seconds)

}