package de.crysxd.octoapp.print_controls.ui.widget.tune

import android.content.Context
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.databinding.WidgetTuneBinding
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.ARG_NO_VALUE
import de.crysxd.octoapp.print_controls.ui.PrintControlsFragmentDirections
import kotlinx.android.synthetic.main.widget_tune.view.*

class TuneWidget(context: Context) : RecyclableOctoWidget<WidgetTuneBinding, TuneWidgetViewModel>(context) {

    override val binding = WidgetTuneBinding.inflate(LayoutInflater.from(context))
    override fun createNewViewModel(parent: WidgetHostFragment) = parent.injectViewModel<TuneWidgetViewModel>().value
    override fun getTitle(context: Context): String? = null
    override fun getAnalyticsName() = "tune"

    init {
        view.setOnClickListener {
            recordInteraction()
            baseViewModel.uiState.value?.let { uiState ->
                it.findNavController().navigate(
                    PrintControlsFragmentDirections.actionTunePrint(
                        currentFanSpeed = uiState.fanSpeed ?: ARG_NO_VALUE,
                        currentFeedRate = uiState.feedRate ?: ARG_NO_VALUE,
                        currentFlowRate = uiState.flowRate ?: ARG_NO_VALUE
                    )
                )
            }
        }
    }

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.uiState.observe(lifecycleOwner, ::updateViewState)
    }

    private fun updateViewState(uiState: TuneWidgetViewModel.UiState) {
        TransitionManager.beginDelayedTransition(view as ViewGroup)

        view.flowRate.isVisible = uiState.flowRate != null
        view.textViewFlowRate.text = context.getString(R.string.x_percent_int, uiState.flowRate)

        view.feedRate.isVisible = uiState.feedRate != null
        view.textViewFeedRate.text = context.getString(R.string.x_percent_int, uiState.feedRate)

        view.fanSpeed.isVisible = uiState.fanSpeed != null
        view.textViewFanSpeed.text = context.getString(R.string.x_percent_int, uiState.fanSpeed)
    }
}