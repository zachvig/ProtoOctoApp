package de.crysxd.octoapp.print_controls.ui.widget.progress

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import kotlinx.coroutines.flow.filter

class ProgressWidgetViewModel(
    octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    val printState = octoPrintProvider.passiveCurrentMessageFlow("progress_widget")
        .filter { it.progress != null }
        .rateLimit(2000)
        .asLiveData()

}