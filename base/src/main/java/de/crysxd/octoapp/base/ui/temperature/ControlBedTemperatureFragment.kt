package de.crysxd.octoapp.base.ui.temperature

import de.crysxd.octoapp.base.di.injectViewModel

class ControlBedTemperatureFragment : ControlTemperatureFragment() {

    override val viewModel: ControlTemperatureViewModelContract by injectViewModel<ControlBedTemperatureViewModel>()

}