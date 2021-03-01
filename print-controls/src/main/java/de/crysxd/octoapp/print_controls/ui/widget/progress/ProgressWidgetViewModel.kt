package de.crysxd.octoapp.base.ui.widget.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.filterEventsForMessageType
import de.crysxd.octoapp.base.ext.smartSample
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.filter

class ProgressWidgetViewModel(
    octoPrintProvider: OctoPrintProvider
) : ViewModel() {

    val printState = octoPrintProvider.eventFlow("progress_widget")
        .filterEventsForMessageType<Message.CurrentMessage>()
        .filter { it.progress != null }
        .smartSample(5000)
        .asLiveData()

}