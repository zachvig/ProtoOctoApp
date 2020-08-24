package de.crysxd.octoapp.print_controls.ui.widget.tune

import android.content.Context
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.Injector
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.ARG_NO_VALUE
import de.crysxd.octoapp.print_controls.ui.PrintControlsFragmentDirections.Companion.actionTunePrint
import kotlinx.android.synthetic.main.widget_tune.view.*

class TuneWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: TuneWidgetViewModel by injectViewModel(Injector.get().viewModelFactory())

    override fun getTitle(context: Context): String? = null
    override fun getAnalyticsName() = "tune"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.suspendedInflate(R.layout.widget_tune, container, false)

    override fun onViewCreated(view: View) {
        viewModel.uiState.observe(viewLifecycleOwner, Observer {
            TransitionManager.beginDelayedTransition(view as ViewGroup)

            view.flowRate.isVisible = it.flowRate != null
            view.textViewFlowRate.text = requireContext().getString(R.string.x_percent_int, it.flowRate)

            view.feedRate.isVisible = it.feedRate != null
            view.textViewFeedRate.text = requireContext().getString(R.string.x_percent_int, it.feedRate)

            view.fanSpeed.isVisible = it.fanSpeed != null
            view.textViewFanSpeed.text = requireContext().getString(R.string.x_percent_int, it.fanSpeed)
        })

        view.setOnClickListener {
            recordInteraction()
            viewModel.uiState.value?.let { uiState ->
                it.findNavController().navigate(
                    actionTunePrint(
                        currentFanSpeed = uiState.fanSpeed ?: ARG_NO_VALUE,
                        currentFeedRate = uiState.feedRate ?: ARG_NO_VALUE,
                        currentFlowRate = uiState.flowRate ?: ARG_NO_VALUE
                    )
                )
            }
        }

        viewModel.updateLiveData.observe(viewLifecycleOwner, Observer { })
    }
}