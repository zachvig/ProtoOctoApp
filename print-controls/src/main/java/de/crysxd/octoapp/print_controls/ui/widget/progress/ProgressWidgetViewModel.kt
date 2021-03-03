package de.crysxd.octoapp.base.ui.widget.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.sample

class ProgressWidgetViewModel(
    octoPrintProvider: OctoPrintProvider
) : ViewModel() {

    val printState = octoPrintProvider.passiveCurrentMessageFlow("progress_widget")
        .filter { it.progress != null && it.progress?.printTime != 0 }
        .sample(5000)
        .asLiveData()

}