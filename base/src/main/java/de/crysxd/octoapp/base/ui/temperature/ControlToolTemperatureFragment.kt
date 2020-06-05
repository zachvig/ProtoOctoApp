package de.crysxd.octoapp.base.ui.temperature

import de.crysxd.octoapp.base.di.injectViewModel

class ControlToolTemperatureFragment : ControlTemperatureFragment() {

    override val viewModel: ControlTemperatureViewModelContract by injectViewModel<ControlToolTemperatureViewModel>()

}