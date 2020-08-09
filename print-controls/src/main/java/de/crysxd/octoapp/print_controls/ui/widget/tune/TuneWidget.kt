package de.crysxd.octoapp.print_controls.ui.widget.tune

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.Injector
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_tune.view.*

class TuneWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: TuneWidgetViewModel by injectViewModel(Injector.get().viewModelFactory())

    override fun getTitle(context: Context): String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View = inflater.inflate(R.layout.widget_tune, container, false)

    override fun onViewCreated(view: View) {
        viewModel.uiState.observe(viewLifecycleOwner, Observer {
            view.textViewFlowRate.text = requireContext().getString(R.string.x_percent_int, it.flowRate)
            view.textViewFeedRate.text = requireContext().getString(R.string.x_percent_int, it.feedRate)
            view.textViewFanSpeed.text = requireContext().getString(R.string.x_percent_int, it.fanSpeed)
        })
    }
}