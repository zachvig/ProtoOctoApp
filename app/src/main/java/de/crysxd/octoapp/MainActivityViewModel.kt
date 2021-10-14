package de.crysxd.octoapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import timber.log.Timber

class MainActivityViewModel : ViewModel() {
    var lastSuccessfulCapabilitiesUpdate = 0L
    var pendingUri: Uri? = null
    var lastNavigation = -1
    var lastWebUrlAndApiKey: String? = "initial"
    var pendingNavigation: Int? = null
    var connectionType: ConnectionType? = null
        set(value) {
            previousConnectionType = field
            Timber.i("Connection type now $value, was $field")
            field = value
        }
    var previousConnectionType: ConnectionType? = null
    var lastFlags: PrinterState.Flags? = null
    var sameFlagsCounter = 0
}