package de.crysxd.octoapp.base.ui.temperature

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requestFocusAndOpenSoftKeyboard
import kotlinx.android.synthetic.main.fragment_control_temperature.*
import kotlinx.android.synthetic.main.view_temperature_input.view.*

abstract class ControlTemperatureFragment : BaseFragment(R.layout.fragment_control_temperature) {

    abstract override val viewModel: ControlTemperatureViewModelContract

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnClickListener {
            viewModel.changeTemperature(requireContext())
        }

        textViewComponentName.setText(viewModel.getComponentName())
        viewModel.temperature.observe(this, Observer {
            val actual = it?.actual?.toInt()?.toString() ?: getString(R.string.no_value_placeholder)
            val target = it?.target?.toInt()?.toString() ?: getString(R.string.no_value_placeholder)
            textViewTemperature.text = getString(R.string.temperature_x_of_y, actual, target)
        })
    }
}