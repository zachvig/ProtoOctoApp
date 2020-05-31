package de.crysxd.octoapp.pre_print_controls.ui

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel

class PrePrintControlsViewModel(
    private val octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    val printerState = octoPrintProvider.printerState

    private fun setTool0Temperature(temp: Float) {

    }

    private fun setBedTemperature(temp: Float) {

    }
}