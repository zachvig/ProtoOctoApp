package de.crysxd.octoapp

import android.net.Uri
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    var lastSuccessfulCapabilitiesUpdate = 0L
    var pendingUri: Uri? = null
    var lastNavigation = -1
    var lastWebUrl: String? = "initial"
    var pendingNavigation: Int? = null
}