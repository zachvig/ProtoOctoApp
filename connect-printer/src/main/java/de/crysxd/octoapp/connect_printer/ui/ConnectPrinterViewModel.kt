package de.crysxd.octoapp.connect_printer.ui

import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel

class ConnectPrinterViewModel(octoPrintProvider: OctoPrintProvider) : BaseViewModel() {

    val printerState = octoPrintProvider.printerState

}