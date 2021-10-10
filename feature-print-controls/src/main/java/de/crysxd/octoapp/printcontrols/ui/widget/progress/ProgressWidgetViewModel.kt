package de.crysxd.octoapp.printcontrols.ui.widget.progress

import androidx.lifecycle.asLiveData
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.network.OctoPrintProvider
import kotlinx.coroutines.flow.filter

class ProgressWidgetViewModel(
    octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    val printState = octoPrintProvider.passiveCurrentMessageFlow("progress_widget")
        .filter { it.progress != null }
        .rateLimit(2000)
        .asLiveData()

}