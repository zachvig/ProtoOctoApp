package de.crysxd.octoapp.base.ui.temperature

import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment

class ControlTemperatureFragment : BaseFragment(R.layout.fragment_control_temperature) {

    override val viewModel: ControlTemperatureViewModel by injectViewModel()

}