package de.crysxd.octoapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.models.printer.PrinterState

class MainActivityViewModel : ViewModel() {
    var lastSuccessfulCapabilitiesUpdate = 0L
    var pendingUri: Uri? = null
    var lastNavigation = -1
    var lastWebUrlAndApiKey: String? = "initial"
    var pendingNavigation: Int? = null
    var connectionType: ConnectionType? = null
    var lastFlags: PrinterState.Flags? = null
    var sameFlagsCounter = 0
}