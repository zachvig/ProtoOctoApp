package de.crysxd.octoapp.base.ui.widget.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.filterEventsForMessageType
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.sample

class ProgressWidgetViewModel(
    octoPrintProvider: OctoPrintProvider
) : ViewModel() {

    val printState = octoPrintProvider.eventFlow("progress_widget")
        .sample(5000)
        .filterEventsForMessageType<Message.CurrentMessage>()
        .filter { it.progress != null }
        .asLiveData()

}